package com.htphatz.profile_service.service;

import com.htphatz.profile_service.dto.request.ProfileRequest;
import com.htphatz.profile_service.dto.response.PageDto;
import com.htphatz.profile_service.dto.response.ProfileResponse;
import com.htphatz.profile_service.entity.Profile;
import com.htphatz.profile_service.exception.AppException;
import com.htphatz.profile_service.exception.ErrorCode;
import com.htphatz.profile_service.mapper.ProfileMapper;
import com.htphatz.profile_service.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    public ProfileResponse createProfile(ProfileRequest request) {
        var existingProfile = profileRepository.findByUserId(request.getUserId());
        if (existingProfile.isPresent()) {
            return profileMapper.toProfileResponse(existingProfile.get());
        }
        Profile profile = profileMapper.toProfile(request);
        return profileMapper.toProfileResponse(profileRepository.save(profile));
    }

    public ProfileResponse getOrCreateMyProfile(Jwt jwt) {
        String userId = jwt.getSubject();

        return profileRepository.findByUserId(userId)
                .map(profileMapper::toProfileResponse)
                .orElseGet(() -> {
                    String firstName = Stream.of(
                                    jwt.getClaimAsString("given_name"),
                                    jwt.getClaimAsString("name"),
                                    jwt.getClaimAsString("preferred_username"))
                            .filter(StringUtils::hasText)
                            .findFirst()
                            .orElse("");
                    ProfileRequest request = ProfileRequest.builder()
                            .userId(userId)
                            .firstName(firstName)
                            .lastName(jwt.getClaimAsString("family_name"))
                            .build();
                    return createProfile(request);
                });
    }

    public PageDto<ProfileResponse> getAllProfiles(Integer pageNumber, Integer pageSize) {
        pageNumber--;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Profile> profiles = profileRepository.findAll(pageable);
        return PageDto.of(profiles.map(profileMapper::toProfileResponse));
    }

    public ProfileResponse getById(String id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));

        return profileMapper.toProfileResponse(profile);
    }

    public ProfileResponse getByUserId(String userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));

        return profileMapper.toProfileResponse(profile);
    }

    public ProfileResponse updateProfile(String id, ProfileRequest request) {
        Profile existingProfile = profileRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));

        existingProfile.setFirstName(request.getFirstName());
        existingProfile.setLastName(request.getLastName());
        existingProfile.setAddress(request.getAddress());
        existingProfile.setPhoneNumber(request.getPhoneNumber());
        return profileMapper.toProfileResponse(profileRepository.save(existingProfile));
    }

    public void deleteProfile(String id) {
        profileRepository.deleteById(id);
    }
}
