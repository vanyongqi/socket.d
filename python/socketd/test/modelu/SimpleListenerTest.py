import uuid
from abc import ABC

from socketd.transport.core.Listener import Listener
from socketd.transport.client.ClientConfig import ClientConfig
from socketd.transport.core.sync_api.AtomicRefer import AtomicRefer
from socketd.transport.server.ServerConfig import ServerConfig
from socketd.transport.core.entity.Entity import Entity
from socketd.transport.core.entity.Message import Message
from socketd.transport.core.entity.StringEntity import StringEntity

from loguru import logger


class SimpleListenerTest(Listener, ABC):

    def __init__(self):
        self.server_counter = AtomicRefer(0)
        self.close_counter = AtomicRefer(0)
        self.message_counter = AtomicRefer(0)

    async def on_open(self, session):
        pass

    async def on_message(self, session, message: Message):
        with self.server_counter:
            self.server_counter.set(self.server_counter.get() + 1)
        if message.is_request():
            with self.message_counter:
                self.message_counter.set(self.message_counter.get() + 1)
            await session.reply(message, StringEntity("reply"))
            await session.reply_end(message, StringEntity("ok test.png"))
            await session.reply(message, StringEntity("reply"))
        elif message.is_subscribe():
            await session.reply(message, StringEntity("reply"))
            await session.reply_end(message, StringEntity("ok test.png"))
            await session.reply(message, StringEntity("reply"))

    def on_close(self, session):
        logger.debug("客户端主动关闭了")
        with self.close_counter:
            self.close_counter.set(self.close_counter.get() + 1)

    def on_error(self, session, error):
        pass


def config_handler(config: ServerConfig | ClientConfig) -> ServerConfig | ClientConfig:
    config.set_is_thread(False)
    config.set_idle_timeout(10)
    config.set_logger_level("DEBUG")
    return config.id_generator(uuid.uuid4)


def send_and_subscribe_test(e: Entity):
    logger.info(e)
