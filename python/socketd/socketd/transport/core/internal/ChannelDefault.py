import asyncio
from typing import  TypeVar
from loguru import logger

from websockets import WebSocketCommonProtocol

from socketd.transport.core.entity.Entity import EntityMetas
from socketd.transport.utils.AssertsUtil import AssertsUtil
from socketd.transport.core.internal.ChannelBase import ChannelBase
from socketd.transport.core.Costants import Function, Flag
from socketd.transport.core.Session import Session
from socketd.transport.core.internal.SessionDefault import SessionDefault
from socketd.transport.core.config.Config import Config
from socketd.transport.core.entity.Frame import Frame
from socketd.transport.core.entity.MessageDefault import MessageDefault
from socketd.transport.core.ChannelAssistant import ChannelAssistant
from socketd.transport.core.stream.StreamBase import StreamBase

S = TypeVar("S", bound=WebSocketCommonProtocol)

_acceptorMap = {}


class ChannelDefault(ChannelBase):

    def __init__(self, source: S, config: Config, assistant: ChannelAssistant):
        ChannelBase.__init__(self, config)
        self.source: WebSocketCommonProtocol = source
        self.assistant = assistant
        self.acceptorMap: dict[str:StreamBase] = dict()
        self.session: Session = None

    def remove_acceptor(self, sid: str):
        if sid in self.acceptorMap:
            self.acceptorMap.pop(sid)

    def is_valid(self) -> bool:
        return self.assistant.is_valid(self.source)

    def get_remote_address(self) -> str:
        return self.assistant.get_remote_address(self.source)

    def get_local_address(self) -> str:
        return self.assistant.get_local_address(self.source)

    async def send(self, frame: Frame, acceptor: StreamBase) -> None:
        AssertsUtil.assert_closed(self)
        if frame.get_message() is not None:
            message = frame.get_message()
            with self:
                if acceptor is not None:
                    self.acceptorMap[message.get_sid()] = acceptor
                if message.get_entity() is not None:
                    if message.get_entity().get_data_size() > self.get_config().get_fragment_size():
                        message.get_meta_map()[EntityMetas.META_DATA_LENGTH] = str(message.get_data_size())

                        fragmentIndex = 0
                        while True:
                            fragmentIndex += 1
                            fragmentEntity = self.get_config().get_fragment_handler().nextFragment(self,
                                                                                                   fragmentIndex,
                                                                                                   message)
                            if fragmentEntity is not None:
                                fragmentFrame = Frame(frame.get_flag(), MessageDefault()
                                                      .set_flag(frame.get_flag())
                                                      .set_sid(message.get_sid())
                                                      .set_entity(fragmentEntity))
                                await self.assistant.write(self.source, fragmentFrame)
                            else:
                                return
                    else:
                        await self.assistant.write(self.source, frame)
                        return

        await self.assistant.write(self.source, frame)

    async def retrieve(self, frame: Frame, onError: Function) -> None:
        acceptor: StreamBase = self.acceptorMap.get(frame.get_message().get_sid())

        if acceptor is not None:
            if acceptor.is_single() or frame.get_flag() == Flag.ReplyEnd:
                self.acceptorMap.pop(frame.get_message().get_sid())
            await asyncio.get_event_loop().run_in_executor(self.get_config().get_executor(),
                                                           lambda _m: acceptor.on_accept(_m, onError),
                                                           frame.get_message())
        else:
            logger.debug(
                f"{self.get_config().get_role_name()} stream not found, sid={frame.get_message().get_sid()}, sessionId={self.get_session().get_session_id()}")

    def get_session(self) -> Session:
        if self.session is None:
            self.session = SessionDefault(self)

        return self.session

    async def close(self, code: int = 1000,
                    reason: str = "", ):
        await super().close()
        self.acceptorMap.clear()
        await self.assistant.close(self.source)

    def on_error(self, error: Exception):
        pass

    def reconnect(self):
        pass

