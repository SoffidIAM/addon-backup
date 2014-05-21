//
// (C) 2013 Soffid
// 
// This file is licensed by Soffid under GPL v3 license
//

package com.soffid.iam.addons.backup.common;
import com.soffid.mda.annotation.*;

@ValueObject 
public abstract class UserBackupConfig {

	@Nullable
	public java.lang.String fullBackupDir;

	@Nullable
	public java.lang.Integer fullBackupHour;

	@Nullable
	public java.lang.Integer fullBackupMinute;

	@Nullable
	public java.lang.String cmdToExecute;

	@Nullable
	public java.lang.Integer userBackupCopies;

	@Description("Minimum number of seconds between backups")
	@Nullable
	public java.lang.Long userBackupDelay;

}
