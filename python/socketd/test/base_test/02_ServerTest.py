import asyncio
import sys

from loguru import logger

from websockets.legacy.server import WebSocketServer
from socketd.transport.core.SocketD import SocketD
from socketd.transport.server.ServerConfig import ServerConfig

from test.modelu.SimpleListenerTest import config_handler, SimpleListenerTest

# logger.remove()
# logger.add(sys.stderr, level="INFO")


async def main():
    server = SocketD.create_server(ServerConfig("ws").set_port(7779))
    server_session: WebSocketServer = await server.config(config_handler).listen(
        SimpleListenerTest()).start()
    await asyncio.Future()
    server_session.close()


if __name__ == "__main__":
    asyncio.run(main())
