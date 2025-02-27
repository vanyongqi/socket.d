import asyncio

from websockets.client import WebSocketClientProtocol
from socketd.transport.core.Channel import Channel
from socketd.transport.core.config.logConfig import logger
from socketd.transport.client.ClientConnectorBase import ClientConnectorBase
from socketd_websocket.impl.AIOConnect import AIOConnect
from socketd_websocket.impl.AIOWebSocketClientImpl import AIOWebSocketClientImpl


class WsAioClientConnector(ClientConnectorBase):
    def __init__(self, client):
        self.real: AIOWebSocketClientImpl = None
        self.__loop = asyncio.get_event_loop()
        self.__stop = asyncio.Future()
        super().__init__(client)
        # nest_asyncio.apply()

    async def connect(self) -> Channel:
        logger.debug('Start connecting to: {}'.format(self.client.get_config().get_url()))

        # 处理自定义架构的影响
        ws_url = self.client.get_config().get_url().replace("std:", "")

        # 支持 ssl
        if self.client.get_config().get_ssl_context() is not None:
            ws_url = ws_url.replace("ws", "wss")
        try:
            con = await AIOConnect(ws_url, client=self.client, ssl=self.client.get_config().get_ssl_context(),
                                   create_protocol=AIOWebSocketClientImpl,
                                   ping_timeout=self.client.get_config().get_idle_timeout(),
                                   # ping_interval=self.client.get_config().get_idle_timeout(),
                                   logger=logger,
                                   max_size=self.client.get_config().get_ws_max_size())
            self.real: AIOWebSocketClientImpl | WebSocketClientProtocol = con
            return self.real.get_channel()
        except Exception as e:
            raise e

    async def close(self):
        if self.real is None:
            return
        try:
            self.real.on_close()
            await self.__stop
        except Exception as e:
            logger.debug(e)
