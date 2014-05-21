//
// (C) 2013 Soffid
// 
// This file is licensed by Soffid under GPL v3 license
//

package com.soffid.iam.addons.backup.model;
import com.soffid.mda.annotation.*;

@Index (name="SCX_BACKUP_USER_NDX",	unique=false,
	entity=com.soffid.iam.addons.backup.model.UserBackupEntity.class,
	columns={"BAC_USERNAME", "BAC_ORDER"})
public abstract class rUserIndex {
}

