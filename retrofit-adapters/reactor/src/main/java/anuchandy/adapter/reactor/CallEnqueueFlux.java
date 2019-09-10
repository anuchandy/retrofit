package anuchandy.adapter.reactor;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.function.Consumer;

final class CallEnqueueFlux {

    static <T> Flux<Response<T>> create(Call<T> originalCall)  {
        return Flux.create(new ResponseFluxSinkConsumer<>(originalCall));
    }

    private CallEnqueueFlux() {
    }

    private static final class ResponseFluxSinkConsumer<T> implements Consumer<FluxSink<Response<T>>> {
        private final Call<T> originalCall;

        ResponseFluxSinkConsumer(Call<T> originalCall) {
            this.originalCall = originalCall;
        }

        @Override
        public void accept(FluxSink<Response<T>> sink) {
            Call<T> call = this.originalCall.clone();
            EnqueueCallBack<T> ecb = new EnqueueCallBack<>(new ResponseFluxSyncWrapper<>(call, sink));
            sink.onRequest(ignored -> {
                if (ecb.isDisposed()) {
                    return;
                }
                call.enqueue(ecb);
            });
            sink.onCancel(() -> ecb.dispose());
        }
    }

    private static final class EnqueueCallBack<T> implements Callback<T>, Disposable {
        private final ResponseFluxSyncWrapper<T> syncWrapper;

        EnqueueCallBack(ResponseFluxSyncWrapper<T> syncWrapper) {
            this.syncWrapper = syncWrapper;
        }

        @Override
        public void onResponse(Call<T> call, Response<T> response) {
            boolean onNextSucceeded = this.syncWrapper.next(response);
            if (onNextSucceeded) {
                this.syncWrapper.complete();
            }
        }

        @Override
        public void onFailure(Call<T> call, Throwable t) {
            if (call.isCanceled()) {
                return;
            }
            this.syncWrapper.error(t);
        }

        @Override
        public void dispose() {
            this.syncWrapper.dispose();
        }

        @Override
        public boolean isDisposed() {
            return this.syncWrapper.isDisposed();
        }
    }
}
