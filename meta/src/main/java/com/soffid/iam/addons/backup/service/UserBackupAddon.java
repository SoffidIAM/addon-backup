//
// (C) 2013 Soffid
// 
// This file is licensed by Soffid under GPL v3 license
//

package com.soffid.iam.addons.backup.service;
import com.soffid.mda.annotation.*;

import org.springframework.transaction.annotation.Transactional;

@Service ( internal=true,
	 translatedName="UserBackupAddon",
	 translatedPackage="com.soffid.iam.addons.backup.service")
public class UserBackupAddon {

	@Transactional(rollbackFor={java.lang.Exception.class})
	public void performBackup(
		java.lang.Long userId, 
		java.lang.StringBuffer sb)
		throws es.caib.seycon.ng.exception.InternalErrorException {
	}
	@Transactional(rollbackFor={java.lang.Exception.class})
	public void restoreBackup(
		es.caib.seycon.ng.model.UsuariEntity user, 
		com.soffid.iam.addons.backup.common.UserBackup backup, 
		java.lang.StringBuffer sb)
		throws es.caib.seycon.ng.exception.InternalErrorException {
	}
}
