package co.com.bancolombia.api;

import co.com.bancolombia.api.broadcast.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class Handler {
    private final BroadcastService broadcastService;

    public Mono<ServerResponse> listenGETUseCase(ServerRequest serverRequest) {
        return ServerResponse.ok().bodyValue("Hello");
    }

    public Mono<ServerResponse> listenGETBroadcast(ServerRequest serverRequest) {
        return broadcastService.broadcast()
                .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }
}
