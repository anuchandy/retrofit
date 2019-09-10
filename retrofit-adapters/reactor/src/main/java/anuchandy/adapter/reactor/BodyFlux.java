package anuchandy.adapter.reactor;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import retrofit2.Response;

import java.util.function.Consumer;

class BodyFlux {
    static <T> Flux<T> create(Flux<Response<T>> upstream)  {
        return Flux.create(new BodyFluxSinkConsumer(upstream));
    }

    private static final class BodyFluxSinkConsumer<T> implements Consumer<FluxSink<T>> {
        private final Flux<Response<T>> upstream;

        BodyFluxSinkConsumer(Flux<Response<T>> upstream) {
            this.upstream = upstream;
        }

        @Override
        public void accept(FluxSink<T> sink) {
            sink.onRequest(ignored -> this.upstream.subscribe(new Subscriber<Response<T>>() {
                private boolean terminated;

                @Override
                public void onSubscribe(Subscription s) {
                    s.request(1);
                }

                @Override
                public void onNext(Response<T> response) {
                    if (response.isSuccessful()) {
                        sink.next(response.body());
                    } else {
                        terminated = true;
                        try {
                            sink.error(new retrofit2.HttpException(response));
                        } catch (Throwable inner) {
                            Exceptions.throwIfFatal(inner);
                            reportUncaughtException(Exceptions.multiple(new retrofit2.HttpException(response), inner));
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    if (!terminated) {
                        sink.error(t);
                    } else {
                        // This should never happen! onNext handles and forwards errors automatically.
                        Throwable broken = new AssertionError(
                                "This should never happen! Report as a bug with the full stacktrace.");
                        //noinspection UnnecessaryInitCause Two-arg AssertionError constructor is 1.7+ only.
                        broken.initCause(t);
                        reportUncaughtException(broken);
                    }
                }

                @Override
                public void onComplete() {
                    if (!this.terminated) {
                        sink.complete();
                    }
                }
            }));
        }

        private static void reportUncaughtException(Throwable error) {
            Thread currentThread = Thread.currentThread();
            Thread.UncaughtExceptionHandler handler = currentThread.getUncaughtExceptionHandler();
            if (handler != null) {
                handler.uncaughtException(currentThread, Exceptions.unwrap(error));
            }
        }
    }
}
