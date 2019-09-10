package anuchandy.adapter.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class ReactorCallAdapterFactory extends CallAdapter.Factory {
    /**
     * Returns an instance which creates synchronous publisher that do not operate on any scheduler by default.
     */
    public static ReactorCallAdapterFactory create() {
        return new ReactorCallAdapterFactory(null, false);
    }

    /**
     * Returns an instance which creates asynchronous publisher. Applying {@link Flux#subscribeOn}
     * or {@link Mono#subscribeOn} has no effect on stream types created by this factory.
     */
    public static ReactorCallAdapterFactory createAsync() {
        return new ReactorCallAdapterFactory(null, true);
    }

    /**
     * Returns an instance which creates synchronous publisher that {@linkplain Flux#subscribeOn(Scheduler) subscribe on}
     * or {@linkplain Mono#subscribeOn(Scheduler) subscribe on} {@code scheduler} by default.
     */
    @SuppressWarnings("ConstantConditions")
    public static ReactorCallAdapterFactory createWithScheduler(Scheduler scheduler) {
        return new ReactorCallAdapterFactory(Objects.requireNonNull(scheduler, "scheduler cannot be null"), false);
    }

    private final @Nullable Scheduler scheduler;
    private final boolean isAsync;

    private ReactorCallAdapterFactory(@Nullable Scheduler scheduler, boolean isAsync) {
        this.scheduler = scheduler;
        this.isAsync = isAsync;
    }

    @Nullable
    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        Class<?> rawType = getRawType(returnType);
        boolean isFlux = rawType == Flux.class;
        boolean isMono = rawType == Mono.class;
        //
        if (!isFlux && !isMono) {
            return null;
        }

        if (!(returnType instanceof ParameterizedType)) {
            String name = isFlux ? "Flux" : "Mono";
            throw new IllegalStateException(String.format("%s return type must be parameterized as in %s<Foo> or %s<? extends Foo>.", name, name, name));
        }

        Type responseType;
        boolean isBody = false;
        Type publisherEmittedType = getParameterUpperBound(0, (ParameterizedType) returnType);
        Class<?> publisherEmittedRawType = getRawType(publisherEmittedType);
        if (publisherEmittedRawType == Response.class) {
            if (!(publisherEmittedType instanceof ParameterizedType)) {
                throw new IllegalStateException("Response must be parameterized as Response<Foo> or Response<? extends Foo>.");
            }
            responseType = getParameterUpperBound(0, (ParameterizedType) publisherEmittedType);
        } else {
            responseType = publisherEmittedType;
            isBody = true;
        }
        return new ReactorCallAdapter(responseType, scheduler, isAsync, isBody, isMono);
    }
}
