package com.soffid.iam.addons.backup.common;

import com.soffid.mda.annotation.Nullable;
import com.soffid.mda.annotation.ValueObject;

@ValueObject
public class UserBackup {
	@Nullable
	public java.lang.Long id;

	public java.util.Calendar backupDate;

	@Nullable
	public java.util.Calendar validUntil;

	public java.lang.String userName;

	public java.lang.Long order;
}
