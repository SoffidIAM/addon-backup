package com.soffid.iam.addons.backup.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import javax.ejb.CreateException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Path;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Window;

import com.soffid.iam.EJBLocator;
import com.soffid.iam.addons.backup.common.UserBackup;
import com.soffid.iam.addons.backup.service.ejb.UserBackupService;
import com.soffid.iam.addons.backup.service.ejb.UserBackupServiceHome;
import com.soffid.iam.api.AccountHistory;
import com.soffid.iam.api.GroupUser;
import com.soffid.iam.api.RoleAccount;
import com.soffid.iam.api.UserMailList;
import com.soffid.iam.service.ejb.AccountService;
import com.soffid.iam.service.ejb.MailListsService;
import com.soffid.iam.web.component.DynamicColumnsDatatable;
import com.soffid.iam.web.component.FrameHandler;
import com.soffid.iam.web.component.SearchBox;
import com.soffid.iam.web.popup.SelectColumnsHandler;

import es.caib.seycon.ng.comu.AccountAccessLevelEnum;
import es.caib.seycon.ng.comu.RolAccount;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.zkib.component.DataModel;
import es.caib.zkib.component.DataTable;
import es.caib.zkib.component.DataTree2;
import es.caib.zkib.component.DateFormats;
import es.caib.zkib.datamodel.DataNodeCollection;
import es.caib.zkib.datasource.CommitException;
import es.caib.zkib.datasource.XPathUtils;
import es.caib.zkib.zkiblaf.Missatgebox;

public class UserBackupHandler extends FrameHandler {
	String parentPath;
	String model;
	
	public UserBackupHandler() throws InternalErrorException {
		super();
	}

	public void selectItem(Event event) {
		DataTable dt = (DataTable) getListbox();
		Button b = (Button) getFellow("restoreButton");
		b.setDisabled(dt.getSelectedIndexes() == null || dt.getSelectedIndexes().length == 0);
	}

	public void doRestore(Event ev) throws InternalErrorException, NamingException, CommitException {
		DataTable dt = (DataTable) getListbox();

		Button b = (Button) getFellow("restoreButton");
		b.setDisabled(dt.getSelectedIndexes() == null || dt.getSelectedIndexes().length == 0);
		if (dt.getSelectedIndex() >= 0) {
		
			if (dt.getDataSource().isCommitPending()) {
				es.caib.zkib.zkiblaf.Missatgebox.info(org.zkoss.util.resource.Labels.getLabel("backup.pendingChange"));
				return;
			}
			
			final UserBackupService backupSvc = (UserBackupService) new InitialContext().lookup(UserBackupServiceHome.JNDI_NAME);
			final UserBackup backup = (UserBackup) XPathUtils.eval(dt, "/instance");

			Missatgebox.confirmaOK_CANCEL(Labels.getLabel("backup.confirmRestore"),
				(event) -> {
					if (event.getName().equals("onOK")) {
						backupSvc.restoreBackup(backup);
						
						es.caib.zkib.zkiblaf.Missatgebox.avis(org.zkoss.util.resource.Labels.getLabel("backup.restoreComplete"));
						
						DataTable parent = getParentListbox();
						
						FrameHandler frame = (FrameHandler) parent.getFellow("frame");
						frame.hideDetails();
						
						SearchBox sb = (SearchBox) parent.getFellow("searchBox");
						sb.search();
					}						
				});
		}	

	}

	public DataTable getParentListbox() {
		return (DataTable) Path.getComponent(getPage(), parentPath);
	}
	
	public void downloadSelected(Event event) throws InternalErrorException, NamingException {
		UserBackup b = (UserBackup) XPathUtils.eval(getListbox(), "instance");
		UserBackupService svc = (UserBackupService) new InitialContext().lookup(UserBackupServiceHome.JNDI_NAME);
		String data = svc.getBackupContent(b);
		Filedownload.save(data, "application/xml", "User backup "+b.getUserName()+"-"+DateFormats.getDateTimeFormat().format(b.getBackupDate().getTime()));
	}

	public void groupHistory(Event event) throws Exception {
		String user = (String) XPathUtils.eval(getParentListbox(), "@userName");
		getModel().getVariables().declareVariable("backupUser", user);
		((DataNodeCollection) getModel().getValue("/groupHistory")).refresh();
		((Window)getFellow("groupsHistoryWindow")).doHighlighted();
	}

	public void roleHistory(Event event) throws Exception {
		String user = (String) XPathUtils.eval(getParentListbox(), "@userName");
		getModel().getVariables().declareVariable("backupUser", user);
		((DataNodeCollection) getModel().getValue("/roleHistory")).refresh();
		((Window)getFellow("rolesHistoryWindow")).doHighlighted();
	}

	public void accountHistory(Event event) throws Exception {
		String user = (String) XPathUtils.eval(getParentListbox(), "@userName");
		getModel().getVariables().declareVariable("backupUser", user);
		((DataNodeCollection) getModel().getValue("/accountHistory")).refresh();
		((Window)getFellow("accountsHistoryWindow")).doHighlighted();
	}

	public String getParentPath() {
		return parentPath;
	}

	public void setParentPath(String parentPath) {
		this.parentPath = parentPath;
	}

	@Override
	protected DataModel getModel() {
		return (DataModel) Path.getComponent(getPage(), model);
	}

	public void setModel(String model) {
		this.model = model;
	}
	
	public void changeColumnsGroups(Event event) throws IOException {
		SelectColumnsHandler.startWizard((DynamicColumnsDatatable) event.getTarget().getFellow("listbox"));
	}
	
	public void downloadCsvGroups(Event event) {
		DataTable dt = (DataTable) event.getTarget().getFellow("listbox");
		dt.download();
	}
	
	public void multiSelectGroup(Event event) {
		DataTable dt = (DataTable) event.getTarget().getFellow("listbox");
		DataModel m = getModel();

		((Button) event.getTarget().getFellow("restoreButton")).setDisabled(dt.getSelectedIndex() < 0);
		int[] selected = dt.getSelectedIndexes();
		for (int i = 0; i < selected.length; i++) {
			int p = selected[i];
			String path = dt.getItemXPath(p);
			GroupUser ra = (GroupUser) XPathUtils.eval(m, path+"/instance");
			
			if (ra.getEnd() == null) {
				int[] selected2 = new int[selected.length-1];
				for (int j = 0; j < selected2.length; j++)
					selected2 [ j ] = selected [ j < i ? j: j+1];
				selected = selected2;
				dt.setSelectedIndex(selected);
			}
		}
		((Button) event.getTarget().getFellow("restoreButton")).setDisabled( selected.length == 0 );
	}

	public void multiSelectAccount(Event event) {
		DataTable dt = (DataTable) event.getTarget().getFellow("listbox");
		DataModel m = getModel();

		((Button) event.getTarget().getFellow("restoreButton")).setDisabled(dt.getSelectedIndex() < 0);
		int[] selected = dt.getSelectedIndexes();
		for (int i = 0; i < selected.length; i++) {
			int p = selected[i];
			String path = dt.getItemXPath(p);
			AccountHistory ra = (AccountHistory) XPathUtils.eval(m, path+"/instance");
			
			if (ra.getEnd() == null) {
				int[] selected2 = new int[selected.length-1];
				for (int j = 0; j < selected2.length; j++)
					selected2 [ j ] = selected [ j < i ? j: j+1];
				selected = selected2;
				dt.setSelectedIndex(selected);
			}
		}
		((Button) event.getTarget().getFellow("restoreButton")).setDisabled( selected.length == 0 );
	}

	public void closeGroups(Event event) {
		Window w = (Window) event.getTarget().getSpaceOwner();
		w.setVisible(false);
	}
	
	public void doRestoreGroups(Event event) {
		final DataTable dt = (DataTable) event.getTarget().getFellow("listbox");

		Button b = (Button) getFellow("restoreButton");
		b.setDisabled(dt.getSelectedIndexes() == null || dt.getSelectedIndexes().length == 0);
		if (dt.getSelectedIndex() >= 0) {
			Missatgebox.confirmaOK_CANCEL(Labels.getLabel("backup.confirmRestore"),
				(event2) -> {
					if (event2.getName().equals("onOK")) {
						DataTable parentListbox = getParentListbox();
						DataModel m = getModel();
						for (int pos: dt.getSelectedIndexes()) {
							String path = dt.getItemXPath(pos);
							GroupUser gu = (GroupUser) XPathUtils.eval(m, path+"/instance");
							if (gu.getDisabled()) {
								GroupUser gu2 = new GroupUser(gu);
								gu2.setId(null);
								XPathUtils.createPath(parentListbox, "/group", gu2);
							}
						}
						getModel().commit();
						dt.setSelectedIndex(-1);
						closeGroups(event);
					}						
				});
		}	
		
	}
	
	public void multiSelectRole(Event event) {
		DataTable dt = (DataTable) event.getTarget().getFellow("listbox");
		DataModel m = getModel();

		((Button) event.getTarget().getFellow("restoreButton")).setDisabled(dt.getSelectedIndex() < 0);
		int[] selected = dt.getSelectedIndexes();
		for (int i = 0; i < selected.length; i++) {
			int p = selected[i];
			String path = dt.getItemXPath(p);
			RoleAccount ra = (RoleAccount) XPathUtils.eval(m, path+"/instance");
			
			if (ra.getRuleId() != null || ra.isEnabled()) {
				int[] selected2 = new int[selected.length-1];
				for (int j = 0; j < selected2.length; j++)
					selected2 [ j ] = selected [ j < i ? j: j+1];
				selected = selected2;
				dt.setSelectedIndex(selected);
			}
		}
		((Button) event.getTarget().getFellow("restoreButton")).setDisabled( selected.length == 0 );
	}


	public void doRestoreRoles(Event event) {
		final DataTable dt = (DataTable) event.getTarget().getFellow("listbox");

		Button b = (Button) getFellow("restoreButton");
		b.setDisabled(dt.getSelectedIndexes() == null || dt.getSelectedIndexes().length == 0);
		if (dt.getSelectedIndexes().length > 0) {
			Missatgebox.confirmaOK_CANCEL(Labels.getLabel("backup.confirmRestore"),
				(event2) -> {
					if (event2.getName().equals("onOK")) {
						DataTable parentListbox = getParentListbox();
						DataModel m = getModel();
						for (int pos: dt.getSelectedIndexes()) {
							String path = dt.getItemXPath(pos);
							RoleAccount ra = (RoleAccount) XPathUtils.eval(m, path+"/instance");
							if (ra.getEndDate() != null) {
								RoleAccount ra2 = new RoleAccount(ra);
								ra2.setId(null);
								ra2.setStartDate(new Date());
								ra2.setEndDate(null);
								XPathUtils.createPath(parentListbox, "/role", ra2);
							}
						}
						getModel().commit();
						dt.setSelectedIndex(-1);
						closeGroups(event);
					}						
				});
		}	
		
	}

	public void doRestoreAccounts(Event event) throws NamingException, CreateException {
		final DataTable dt = (DataTable) event.getTarget().getFellow("listbox");
		final AccountService svc = EJBLocator.getAccountService();
		final String user = (String) XPathUtils.eval(getParentListbox(), "/@userName");
		
		Button b = (Button) getFellow("restoreButton");
		b.setDisabled(dt.getSelectedIndexes() == null || dt.getSelectedIndexes().length == 0);
		if (dt.getSelectedIndex() >= 0) {
			Missatgebox.confirmaOK_CANCEL(Labels.getLabel("backup.confirmRestore"),
				(event2) -> {
					if (event2.getName().equals("onOK")) {
						DataTable parentListbox = getParentListbox();
						DataModel m = getModel();
						for (int pos: dt.getSelectedIndexes()) {
							String path = dt.getItemXPath(pos);
							AccountHistory ah = (AccountHistory) XPathUtils.eval(m, path+"/instance");
							if (ah.getEnd() != null) {
								if (ah.getLevel() == AccountAccessLevelEnum.ACCESS_OWNER) {
									ah.getAccount().getOwnerUsers().add(user);
									svc.updateAccount(ah.getAccount());
								}
								if (ah.getLevel() == AccountAccessLevelEnum.ACCESS_MANAGER) {
									ah.getAccount().getManagerUsers().add(user);
									svc.updateAccount(ah.getAccount());
								}
								if (ah.getLevel() == AccountAccessLevelEnum.ACCESS_USER) {
									ah.getAccount().getGrantedUsers().add(user);
									svc.updateAccount(ah.getAccount());
								}
							}
						}
						getModel().commit();
						DataNodeCollection call = (DataNodeCollection) XPathUtils.eval(getParentListbox(), "/sharedAccount");
						call.refresh();
						closeGroups(event);
					}						
				});
		}	
		
	}

	public void doRestoreMail(Event event) throws NamingException, CreateException {
		final DataTable dt = (DataTable) event.getTarget().getFellow("listbox");
		final MailListsService svc = EJBLocator.getMailListsService();
		final String user = (String) XPathUtils.eval(getParentListbox(), "/@userName");
		
		Button b = (Button) getFellow("restoreButton");
		b.setDisabled(dt.getSelectedIndexes() == null || dt.getSelectedIndexes().length == 0);
		if (dt.getSelectedIndex() >= 0) {
			Missatgebox.confirmaOK_CANCEL(Labels.getLabel("backup.confirmRestore"),
				(event2) -> {
					if (event2.getName().equals("onOK")) {
						DataTable parentListbox = getParentListbox();
						DataModel m = getModel();
						for (int pos: dt.getSelectedIndexes()) {
							String path = dt.getItemXPath(pos);
							UserMailList ah = (UserMailList) XPathUtils.eval(m, path+"/instance");
							if (ah.getEnd() != null) {
								UserMailList um = new UserMailList(ah);
								um.setId(null);
								um.setEnd(null);
								svc.create(um);
							}
						}
						DataTable parent = getParentListbox();
						SearchBox sb = (SearchBox) parent.getFellow("searchBox");
						sb.search();
						closeGroups(event);
					}						
				});
		}	
		
	}

	public void multiSelectMail(Event event) {
		DataTable dt = (DataTable) event.getTarget().getFellow("listbox");
		DataModel m = getModel();

		((Button) event.getTarget().getFellow("restoreButton")).setDisabled(dt.getSelectedIndex() < 0);
		int[] selected = dt.getSelectedIndexes();
		for (int i = 0; i < selected.length; i++) {
			int p = selected[i];
			String path = dt.getItemXPath(p);
			UserMailList ra = (UserMailList) XPathUtils.eval(m, path+"/instance");
			
			if (ra.getEnd() == null) {
				int[] selected2 = new int[selected.length-1];
				for (int j = 0; j < selected2.length; j++)
					selected2 [ j ] = selected [ j < i ? j: j+1];
				selected = selected2;
				dt.setSelectedIndex(selected);
			}
		}
		((Button) event.getTarget().getFellow("restoreButton")).setDisabled( selected.length == 0 );
	}

	public void mailHistory(Event event) throws Exception {
		String user = (String) XPathUtils.eval(getParentListbox(), "@userName");
		getModel().getVariables().declareVariable("backupUser", user);
		((DataNodeCollection) getModel().getValue("/mailHistory")).refresh();
		((Window)getFellow("mailHistoryWindow")).doHighlighted();
	}

}
