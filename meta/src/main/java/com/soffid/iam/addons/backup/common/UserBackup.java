package com.soffid.iam.addons.backup.common;

import com.soffid.iam.addons.backup.model.UserBackupEntity;
import com.soffid.mda.annotation.Attribute;
import com.soffid.mda.annotation.JsonAttribute;
import com.soffid.mda.annotation.JsonObject;
import com.soffid.mda.annotation.Nullable;
import com.soffid.mda.annotation.ValueObject;

@ValueObject
@JsonObject(hibernateClass = UserBackupEntity.class)
public class UserBackup {
	@Nullable
	public java.lang.Long id;

	public java.util.Calendar backupDate;

	@Nullable
	public java.util.Calendar validUntil;

	public java.lang.String userName;

}
