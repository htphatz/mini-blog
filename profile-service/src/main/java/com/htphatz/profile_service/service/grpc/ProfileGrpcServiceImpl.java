package com.htphatz.profile_service.service.grpc;

import com.htphatz.profile.grpc.ProfileGrpcServiceGrpc;
import com.htphatz.profile.grpc.ProfileRequest;
import com.htphatz.profile.grpc.ProfileResponse;
import com.htphatz.profile_service.repository.ProfileRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class ProfileGrpcServiceImpl extends ProfileGrpcServiceGrpc.ProfileGrpcServiceImplBase {

    private final ProfileRepository profileRepository;

    @Override
    public void getProfileByUserId(ProfileRequest request, StreamObserver<ProfileResponse> responseObserver) {
        log.info("Received gRPC request to get profile by userId: {}", request.getUserId());

        var profileOpt = profileRepository.findByUserId(request.getUserId());

        if (profileOpt.isEmpty()) {
            log.warn("Profile not found for userId: {}", request.getUserId());
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Profile not found for userId: " + request.getUserId())
                            .asRuntimeException()
            );
            return;
        }

        var profile = profileOpt.get();
        ProfileResponse response = ProfileResponse.newBuilder()
                .setId(profile.getId() != null ? profile.getId() : "")
                .setUserId(profile.getUserId() != null ? profile.getUserId() : "")
                .setFirstName(profile.getFirstName() != null ? profile.getFirstName() : "")
                .setLastName(profile.getLastName() != null ? profile.getLastName() : "")
                .setPhoneNumber(profile.getPhoneNumber() != null ? profile.getPhoneNumber() : "")
                .setAddress(profile.getAddress() != null ? profile.getAddress() : "")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
