package com.htphatz.post_service.service.grpc;

import com.htphatz.post_service.dto.response.ProfileResponse;
import com.htphatz.post_service.repository.httpclient.ProfileClient;
import com.htphatz.profile.grpc.ProfileGrpcServiceGrpc;
import com.htphatz.profile.grpc.ProfileRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileGrpcClient {

    @GrpcClient("profile-service")
    private ProfileGrpcServiceGrpc.ProfileGrpcServiceBlockingStub profileStub;

    private final ProfileClient feignProfileClient;

    public ProfileResponse getProfileByUserId(String userId) {
        try {
            log.info("Fetching profile via high-performance gRPC for userId: {}", userId);
            var grpcRequest = ProfileRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            var grpcResponse = profileStub.getProfileByUserId(grpcRequest);

            return ProfileResponse.builder()
                    .id(grpcResponse.getId())
                    .userId(grpcResponse.getUserId())
                    .firstName(grpcResponse.getFirstName())
                    .lastName(grpcResponse.getLastName())
                    .phoneNumber(grpcResponse.getPhoneNumber())
                    .address(grpcResponse.getAddress())
                    .build();
        } catch (Exception ex) {
            log.warn("gRPC call to profile-service failed ({}), falling back to OpenFeign REST: {}",
                    ex.getClass().getSimpleName(), ex.getMessage());
            return feignProfileClient.getByUserId(userId);
        }
    }
}
