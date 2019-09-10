Reactor Adapter
==============

An `Adapter` for adapting Reactor types.

Available types:

 * `Flux<T>` and `Flux<Response<T>>` where `T` is the body type.
 * `Mono<T>` and `Mono<Response<T>>` where `T` is the body type.

Usage
-----

Add `ReactorCallAdapterFactory` as a `Call` adapter when building your `Retrofit` instance:
```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://example.com/")
    .addCallAdapterFactory(ReactorCallAdapterFactory.create())
    .build();
```

Your service methods can now use any of the above types as their return type.
```java
interface MyService {
  @GET("/user")
  Flux<User> getUser();
}
```

By default all reactive types execute their requests synchronously. There are multiple ways to
control the threading on which a request occurs:

 * Call `subscribeOn` on the returned reactive type with a `Scheduler` of your choice.
 * Use `createAsync()` when creating the factory which will use OkHttp's internal thread pool.
 * Use `createWithScheduler(Scheduler)` to supply a default subscription `Scheduler`.

