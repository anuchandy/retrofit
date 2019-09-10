package anuchandy.adapter.reactor;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;

import javax.annotation.Nullable;
import java.lang.reflect.Type;

final class ReactorCallAdapter<R> implements CallAdapter<R, Object> {
    private final Type responseType;
    private final @Nullable Scheduler scheduler;
    private final boolean isAsync;
    private final boolean isBody;
    private final boolean isMono;

    ReactorCallAdapter(Type responseType, @Nullable Scheduler scheduler, boolean isAsync,
                       boolean isBody, boolean isMono) {
        this.responseType = responseType;
        this.scheduler = scheduler;
        this.isAsync = isAsync;
        this.isBody = isBody;
        this.isMono = isMono;
    }

    @Override
    public Type responseType() {
        return this.responseType;
    }

    @Override
    public Object adapt(Call<R> call) {
        Flux<Response<R>> responseFlux = this.isAsync
                ? CallEnqueueFlux.create(call)
                : CallExecuteFlux.create(call);
        Flux<?> flux = this.isBody
                ? BodyFlux.create(responseFlux)
                : responseFlux;
        if (scheduler != null) {
            flux = flux.subscribeOn(scheduler);
        }
        return this.isMono ? flux.last() : flux;
    }
}
