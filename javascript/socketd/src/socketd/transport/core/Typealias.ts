
export type IoConsumer<T> = (t:T) => void
export type IoBiConsumer<T1,T2> = (t1:T1, t2:T2 ) => void
export type IoFunction<T1,T2> = (t1:T1) => T2;
