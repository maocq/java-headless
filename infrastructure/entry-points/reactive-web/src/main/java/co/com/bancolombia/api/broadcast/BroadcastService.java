package co.com.bancolombia.api.broadcast;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.util.List;
import java.util.function.Supplier;

@Service
public class BroadcastService {

    private final Supplier<WebClient> webClientFunction;
    private final String namespace;
    private final String url;

    public BroadcastService(
            Supplier<WebClient> webClientFunction,
            @Value("${broadcast.namespace}") String namespace,
            @Value("${broadcast.url}") String url) {
        this.webClientFunction = webClientFunction;
        this.namespace = namespace;
        this.url = url;
    }

    public Mono<List<String>> broadcast() {
        var myIp = getMyHostAddress();

        return Mono.fromSupplier(webClientFunction)
                .flatMapMany(webClient -> Flux.just(getAddresses())
                .flatMap(address -> {
                    var podUrl = String.format(url, address.getHostAddress());
                    String okurl =  myIp.equals(address.getHostAddress())
                            ? podUrl.replace("https", "http") : podUrl;
                    return executeRequest(webClient, okurl);
                })).collectList();
    }

    private Mono<String> executeRequest(WebClient webClient, String url) {
        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> url + ": " + response)
                .onErrorResume(error -> Mono.just(url + ": " + error.getMessage()));
    }

    @SneakyThrows
    private InetAddress[] getAddresses() {
        return InetAddress.getAllByName(namespace);
    }

    @SneakyThrows
    private static String getMyHostAddress() {
        return InetAddress.getLocalHost().getHostAddress();
    }
}
