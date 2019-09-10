package anuchandy.adapter.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import retrofit2.Call;
import retrofit2.Response;

import java.util.function.Consumer;

final class CallExecuteFlux {
    static <T> Flux<Response<T>> create(Call<T> originalCall)  {
        return Flux.create(new ResponseFluxSinkConsumer<>(originalCall));
    }

    private CallExecuteFlux() {
    }

    private static final class ResponseFluxSinkConsumer<T> implements Consumer<FluxSink<Response<T>>> {
        private final Call<T> originalCall;

        ResponseFluxSinkConsumer(Call<T> originalCall) {
            this.originalCall = originalCall;
        }

        @Override
        public void accept(FluxSink<Response<T>> sink) {
            Call<T> call = this.originalCall.clone();
            ResponseFluxSyncWrapper<T> syncWrapper = new ResponseFluxSyncWrapper<>(call, sink);
            sink.onRequest(ignored -> {
                if (syncWrapper.isDisposed()) {
                    return;
                }

                Response<T> response;
                try {
                    response = call.execute();
                } catch (Throwable t) {
                    syncWrapper.error(t);
                    return;
                }

                boolean onNextSucceeded = syncWrapper.next(response);
                if (onNextSucceeded) {
                    syncWrapper.complete();
                }
            });
            sink.onCancel(() -> syncWrapper.dispose());
        }
    }
}
