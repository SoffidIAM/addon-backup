//
// (C) 2013 Soffid
// 
// This file is licensed by Soffid under GPL v3 license
//

package com.soffid.iam.addons.backup.service;
import com.soffid.mda.annotation.*;

import org.springframework.transaction.annotation.Transactional;

@Service ( translatedName="BackupBootstrapService",
	 translatedPackage="com.soffid.iam.addons.backup.service")
@Depends ({com.soffid.iam.addons.backup.service.UserBackupService.class})
public class BackupBootstrapService extends es.caib.seycon.ng.servei.ApplicationBootService {

}
