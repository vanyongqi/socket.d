from abc import ABC, abstractmethod

from socketd.transport.core.Listener import Listener


class Processor(Listener, ABC, ):

    @abstractmethod
    def set_listener(self, listener):
        pass

    @abstractmethod
    def on_receive(self, channel, frame):
        pass
