package io.github.kingg22.ktorgen.example;

import io.github.kingg22.ktorgen.http.*;
import kotlinx.coroutines.flow.Flow;

import java.util.List;

public interface Github {
    @GET("repos/{owner}/{repo}")
    @Header(name = "Accept", value = "application/json")
    @Header(name = Header.Accept, value = Header.ContentTypes.Application.Cbor)
    Flow<String> getRepo(@Path String owner, @Path String repo, @HeaderParam(name = "Authorization") String token);

    @Cookie(
        name = "session_id",
        value = "abc123",
        maxAge = 3600,
        expiresTimestamp = 1_735_689_600_000L, // 01/01/2025 00:00:00 GMT
        secure = true,
        httpOnly = true,
        extensions = {@Cookie.PairString(first = Cookie.SameSite, second = Cookie.SameSites.Strict)}
    )
    @Cookie(name = "session_id", value = "abc123")
    @GET("users")
    Flow<List<String>> getUsers(
        @Query(value = "page", encoded = true) int page,
        @Cookie(name = "session_id") @Cookie(name = "session") int session
    );
}
