from socketd.transport.core.entity.Message import Message


class FragmentHolder:

    def __init__(self, index: int, message: Message):
        self.index = index
        self.message = message
