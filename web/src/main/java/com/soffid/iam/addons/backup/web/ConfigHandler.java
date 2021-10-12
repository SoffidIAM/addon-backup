package com.soffid.iam.addons.backup.web;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Window;

import com.soffid.iam.addons.backup.common.UserBackup;
import com.soffid.iam.addons.backup.service.ejb.UserBackupService;
import com.soffid.iam.addons.backup.service.ejb.UserBackupServiceHome;
import com.soffid.iam.web.component.FrameHandler;

import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.zkib.component.DataTable;
import es.caib.zkib.component.DateFormats;
import es.caib.zkib.datasource.XPathUtils;

public class ConfigHandler extends FrameHandler {

	public ConfigHandler() throws InternalErrorException {
		super();
	}

	public void configure(Event event) {
		Window w = (Window) getFellow("configureWindow");
		w.doHighlighted();
	}
	
	public void selectItem(Event event) {
		DataTable dt = (DataTable) getListbox();
		Button b = (Button) getFellow("restoreButton");
		b.setDisabled(dt.getSelectedIndexes() == null || dt.getSelectedIndexes().length == 0);
	}
	
	public void doRestore(Event ev) throws InternalErrorException, NamingException {
		boolean any = false;
		boolean done = false;

		DataTable dt = (DataTable) getListbox();
		es.caib.zkib.component.DataModel model = getModel();
		
		if (model.isCommitPending()) {
			es.caib.zkib.zkiblaf.Missatgebox.info(org.zkoss.util.resource.Labels.getLabel("backup.pendingChange"));
			return;
		}
		
		UserBackupService backupSvc = (UserBackupService) new InitialContext().lookup(UserBackupServiceHome.JNDI_NAME);
		for (int position: dt.getSelectedIndexes()) {
			String path = dt.getItemXPath(position);
			UserBackup b = (UserBackup) model.getValue(path+"/instance");
			backupSvc.restoreBackup(b);
		}


		es.caib.zkib.zkiblaf.Missatgebox.avis(org.zkoss.util.resource.Labels.getLabel("backup.restoreComplete"));

		model.refresh();
	}

	
	public void downloadSelected(Event event) throws InternalErrorException, NamingException {
		UserBackup b = (UserBackup) XPathUtils.eval(getListbox(), "instance");
		UserBackupService svc = (UserBackupService) new InitialContext().lookup(UserBackupServiceHome.JNDI_NAME);
		String data = svc.getBackupContent(b);
		Filedownload.save(data, "application/xml", "User backup "+b.getUserName()+"-"+DateFormats.getDateTimeFormat().format(b.getBackupDate().getTime()));
	}
}
