import asyncio
import uuid
from abc import ABC

from websockets.legacy.server import WebSocketServer
from loguru import logger
from pathlib import Path

from socketd.transport.core.Listener import Listener
from socketd.transport.client.ClientConfig import ClientConfig
from socketd.transport.core.entity.Entity import EntityMetas
from socketd.transport.core.entity.FileEntity import FileEntity
from socketd.transport.core.entity.Message import Message
from socketd.transport.core.sync_api.AtomicRefer import AtomicRefer
from test.modelu.BaseTestCase import BaseTestCase

from socketd.transport.core.Session import Session
from socketd.transport.core.SocketD import SocketD
from socketd.transport.server.ServerConfig import ServerConfig
from socketd.transport.server.Server import Server


def config_handler(config: ServerConfig | ClientConfig) -> ServerConfig | ClientConfig:
    config.set_is_thread(False)
    config.set_ws_max_size(2 ** 20 * 17)
    return config.id_generator(uuid.uuid4)


class SimpleListenerTest(Listener, ABC):

    def __init__(self):
        self.message_counter = AtomicRefer(0)

    async def on_open(self, session):
        pass

    async def on_message(self, session, message: Message):
        logger.debug(message)
        with self.message_counter:
            self.message_counter.set(self.message_counter.get() + 1)

        file_name = message.get_meta(EntityMetas.META_DATA_DISPOSITION_FILENAME)
        out_file_name = "./test.png"
        if file_name:
            logger.debug(f"file_name {file_name}")
            with open(out_file_name, "wb") as f:
                f.write(message.get_data_as_bytes())

        path = Path(out_file_name)
        assert path.exists()

    def on_close(self, session):
        pass

    def on_error(self, session, error):
        pass


class TestCase05_file(BaseTestCase):

    def __init__(self, schema, port):
        super().__init__(schema, port)
        self.server: Server
        self.server_session: WebSocketServer
        self.client_session: Session
        self.loop = asyncio.get_event_loop()
        self._simple = SimpleListenerTest()

    async def _start(self):
        self.server: Server = SocketD.create_server(ServerConfig(self.schema).set_port(self.port))
        _server = self.server.config(config_handler).listen(self._simple)
        self.server_session: WebSocketServer = await _server.start()
        await asyncio.sleep(1)
        serverUrl = self.schema + "://127.0.0.1:" + str(self.port) + "/path?u=a&p=2"
        self.client_session: Session = await SocketD.create_client(serverUrl) \
            .config(config_handler).open()
        try:
            with open(r"C:\Users\bai\Pictures\飞书20230728-180708.mp4",
                      "rb") as f:
                await self.client_session.send("/path?u=a&p=2", FileEntity(f.read(), "test.png"))
        except Exception as e:
            logger.error(e)
            raise e
        await asyncio.sleep(10)

    def start(self):
        super().start()
        self.loop.run_until_complete(self._start())
        logger.info(
            f" message {self._simple.message_counter.get()}")

    async def _stop(self):
        if self.client_session:
            await self.client_session.close()

        if self.server_session:
            self.server_session.close()
        if self.server:
            await self.server.stop()

    def stop(self):
        super().stop()
        self.loop.run_until_complete(self._stop())

    def on_error(self):
        super().on_error()
