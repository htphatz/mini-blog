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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    public ProfileResponse createProfile(ProfileRequest request) {
        Profile profile = profileMapper.toProfile(request);
        return profileMapper.toProfileResponse(profileRepository.save(profile));
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

    public void deleteProfile(String id) {
        profileRepository.deleteById(id);
    }
}
