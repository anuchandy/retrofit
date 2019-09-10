package anuchandy.adapter.reactor;

import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.FluxSink;
import retrofit2.Call;
import retrofit2.Response;

final class ResponseFluxSyncWrapper<T> implements Disposable {
    private final Call<T> call;
    private final FluxSink<Response<T>> innerSink;
    private volatile boolean disposed;

    ResponseFluxSyncWrapper(Call<T> call, FluxSink<Response<T>> innerSink) {
        this.call = call;
        this.innerSink = innerSink;
    }

    @Override
    public void dispose() {
        this.disposed = true;
        this.call.cancel();
    }

    @Override
    public boolean isDisposed() {
        return this.disposed;
    }

    public boolean next(Response<T> response) {
        if (this.disposed) {
            return false;
        }
        try {
            this.innerSink.next(response);
            return true;
        } catch (Throwable t) {
            Exceptions.throwIfFatal(t);
            if (!this.disposed) {
                this.error(t);
            }
            return false;
        }
    }

    public void complete() {
        if (this.disposed) {
            return;
        }
        try {
            this.innerSink.complete();
        } catch (Throwable t) {
            Exceptions.throwIfFatal(t);
            reportUncaughtException(t);
        }
    }

    public void error(Throwable t) {
        try {
            this.innerSink.error(t);
        } catch (Throwable inner) {
            Exceptions.throwIfFatal(inner);
            reportUncaughtException(Exceptions.multiple(t, inner));
        }
    }

    private static void reportUncaughtException(Throwable error) {
        Thread currentThread = Thread.currentThread();
        Thread.UncaughtExceptionHandler handler = currentThread.getUncaughtExceptionHandler();
        if (handler != null) {
            handler.uncaughtException(currentThread, Exceptions.unwrap(error));
        }
    }
}
