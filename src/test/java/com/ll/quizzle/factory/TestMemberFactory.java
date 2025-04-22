package com.ll.quizzle.factory;

import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.avatar.entity.OwnedAvatar;
import com.ll.quizzle.domain.avatar.repository.AvatarRepository;
import com.ll.quizzle.domain.avatar.repository.OwnedAvatarRepository;
import com.ll.quizzle.domain.avatar.type.AvatarTemplate;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.global.exceptions.ErrorCode;
import com.ll.quizzle.global.security.oauth2.entity.OAuth;
import com.ll.quizzle.global.security.oauth2.repository.OAuthRepository;

public class TestMemberFactory {

	public static Member createOAuthMember(
		String nickname,
		String email,
		String provider,
		String oauthId,
		MemberRepository memberRepo,
		OAuthRepository oauthRepo,
		AvatarRepository avatarRepo,
		OwnedAvatarRepository ownedAvatarRepo) {

		Avatar defaultAvatar = avatarRepo.findByFileName(AvatarTemplate.DEFAULT.fileName)
			.orElseThrow(ErrorCode.AVATAR_NOT_FOUND::throwServiceException);

		Member member = Member.create(nickname, email, defaultAvatar);
		memberRepo.save(member);

		OAuth oauth = OAuth.create(member, provider, oauthId);
		oauthRepo.save(oauth);

		if (!member.hasAvatar(defaultAvatar)) {
			OwnedAvatar owned = OwnedAvatar.create(member, defaultAvatar);
			member.addOwnedAvatar(owned);
			ownedAvatarRepo.save(owned);
		}

		member.changeAvatar(defaultAvatar);
		memberRepo.save(member);

		return member;
	}
}
