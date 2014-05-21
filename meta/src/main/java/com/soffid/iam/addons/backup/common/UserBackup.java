//
// (C) 2013 Soffid
// 
// This file is licensed by Soffid under GPL v3 license
//

package com.soffid.iam.addons.backup.common;
import com.soffid.mda.annotation.*;

@ValueObject 
public abstract class UserBackup {

	@Nullable
	public java.lang.Long id;

	public java.util.Calendar backupDate;

	@Nullable
	public java.util.Calendar validUntil;

	public java.lang.String userName;

	public java.lang.Long order;

}
