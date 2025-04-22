package com.ll.quizzle.domain.point.type;

public enum PointReason {
	NICKNAME_CHANGE("닉네임 변경", -100),
	LEVEL_UP("레벨업 보상", +500),
	AVATAR_PURCHASE("아바타 구매", -1);

	private final String description;
	private final int defaultAmount;

	PointReason(String description, int defaultAmount) {
		this.description = description;
		this.defaultAmount = defaultAmount;
	}

	public String getDescription() {
		return description;
	}

	public int getDefaultAmount() {
		return defaultAmount;
	}
}

