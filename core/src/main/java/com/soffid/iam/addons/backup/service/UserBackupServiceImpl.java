package com.soffid.iam.addons.backup.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soffid.iam.addons.backup.common.UserBackup;
import com.soffid.iam.addons.backup.common.UserBackupConfig;
import com.soffid.iam.addons.backup.model.UserBackupEntity;
import com.soffid.iam.api.Configuration;
import com.soffid.iam.api.DomainValue;
import com.soffid.iam.api.GroupUser;
import com.soffid.iam.api.Role;
import com.soffid.iam.api.RoleAccount;
import com.soffid.iam.api.User;
import com.soffid.iam.api.UserAccount;
import com.soffid.iam.api.UserData;
import com.soffid.iam.api.System;
import com.soffid.iam.model.AccountEntity;
import com.soffid.iam.model.UserAccountEntity;
import com.soffid.iam.model.UserEntity;
import com.soffid.iam.utils.ConfigurationCache;

import es.caib.seycon.ng.comu.AccountType;
import es.caib.seycon.ng.exception.AccountAlreadyExistsException;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.seycon.ng.exception.NeedsAccountNameException;
import es.caib.seycon.ng.utils.Security;
import es.caib.seycon.util.Base64;

public class UserBackupServiceImpl extends UserBackupServiceBase implements ApplicationContextAware {

	private static final String VALUE = "value";
	private static final String TAG = "tag";
	private static final String CUSTOM_DATA = "customData";
	private static final String GROUP_MEMBERSHIP = "groupMembership";
	private static final String DOMAIN = "domain";
	private static final String SYSTEM = "system";
	private static final String NAME = "name";
	private static final String ROLE = "role";
	private static final String ACCOUNT = "account";
	private static final String USER = "user";
	private static final String USER_BACKUP = "user-backup";
	DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private ApplicationContext applicationContext;
			
	@Override
	protected void handlePerformBackup(String userId) throws Exception {
		UserEntity user = getUserEntityDao().findByUserName(userId);
		if (user == null)
			return;
		
		Security.nestedLogin(Security.getCurrentAccount(), Security.ALL_PERMISSIONS);
		String xml;
		try{
			xml = generateXml (user);
		} finally {
			Security.nestedLogoff();
		}
		long order = 0;
		Date now = new Date();
		List<UserBackupEntity> oldBackups = getUserBackupEntityDao().findByUser(user.getUserName());
		
		// Sort from high to low
		Collections.sort(oldBackups, new Comparator<UserBackupEntity>() {

			public int compare(UserBackupEntity o1, UserBackupEntity o2) {
				return o2.getOrder().compareTo(o1.getOrder());
			}
		});

		UserBackupConfig config = getConfig();
		
		if (oldBackups.size() > 0)
		{
			UserBackupEntity last = oldBackups.get(oldBackups.size()-1);
			if (last.getData().equals(xml))
				return ;
			if (now.getTime() - last.getBackupDate().getTime() <  config.getUserBackupDelay() * 1000)
			{
				last.setData(xml);
				getUserBackupEntityDao().update(last);
				return;
			}
		}
		
		for (UserBackupEntity backup: oldBackups)
		{
			if (backup.getOrder().longValue() >= config.getUserBackupCopies().intValue() - 1)
			{
				getUserBackupEntityDao().remove(backup);
			}
			else
			{
				backup.setOrder(backup.getOrder()+1);
				if (backup.getValidUntil() == null)
					backup.setValidUntil(now);
				getUserBackupEntityDao().update(backup);
			}
		}
		
		UserBackupEntity backup = getUserBackupEntityDao().newUserBackupEntity();
		backup.setBackupDate(new Date());
		backup.setData(xml);
		backup.setOrder(new Long(0));
		backup.setUserName(user.getUserName());
		getUserBackupEntityDao().create(backup);
	}

	@Override
	protected void handlePerformBackup(String userId, String system) throws Exception {
		AccountEntity account = getAccountEntityDao().findByNameAndSystem(userId, system);
		
		if (account != null && account.getType().equals(AccountType.USER))
		{
			for ( UserAccountEntity u: account.getUsers())
			{
				handlePerformBackup(u.getUser().getUserName());
			}
		}
	}

	private String generateXml(UserEntity userEntity) throws InternalErrorException {
		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
			.append("\n");
		
		serialize (sb, 0, USER_BACKUP, null, false);
		User user = getUserEntityDao().toUser(userEntity);
		
		serialize (sb, 2, USER, user, true);
		
		for (UserAccount account: getAccountService().getUserAccounts(user))
		{
			if (!account.isDisabled())
			{
				serialize (sb, 2, ACCOUNT, null, false);
				serialize (sb, 4, NAME, account.getName(), true);
				serialize (sb, 4, SYSTEM, account.getSystem(), true);
				for (RoleAccount RoleAccount: getApplicationService().findRoleAccountByAccount(account.getId()))
				{
					serialize (sb, 4, ROLE, null, false);
					serialize (sb, 6, NAME, RoleAccount.getRoleName(), true);
					serialize (sb, 6, SYSTEM, RoleAccount.getSystem(), true);
					if (RoleAccount.getDomainValue() != null)
						serialize (sb, 6, DOMAIN, RoleAccount.getDomainValue().getValue(), true);
					closeTag (sb, 4, ROLE);
				}
				closeTag (sb, 2, ACCOUNT);
			}
		}
		
		for (GroupUser userGrup: getGroupService().findUsersGroupByUserName(userEntity.getUserName()))
		{
			serialize (sb, 2, GROUP_MEMBERSHIP, userGrup.getGroup(), true);
		}
		
		for (UserData dada: getUserService().findUserDataByUserName(userEntity.getUserName()))
		{
			if (dada.getValue() != null)
			{
				serialize (sb, 2, CUSTOM_DATA, null, false);
				serialize (sb, 4, TAG, dada.getAttribute(), true);
				serialize (sb, 4, VALUE, dada.getValue(), true);
				closeTag(sb, 2, CUSTOM_DATA);
			}
		}

		closeTag (sb, 0, USER_BACKUP);
		
		return sb.toString();
	}

	private void closeTag (StringBuffer sb, int indent, String tag)
	{
		for (int i = 0; i < indent; i++)
			sb.append (' ');
		sb.append ("</").append(tag).append(">");
		sb.append ('\n');
	}
	private void serialize(StringBuffer sb, int indent, String tag, Object obj, boolean closeTag) {
		for (int i = 0; i < indent; i++)
			sb.append (' ');
		sb.append ("<").append(tag).append(">");
		if (obj == null)
		{
		}
		else if (obj instanceof String || obj instanceof Long || 
				obj instanceof Integer || obj instanceof Boolean)
		{
			sb.append (unscape (obj.toString()));
		}
		else if (obj instanceof Date)
		{
			sb.append (dateformat.format(obj));
		}
		else if (obj instanceof Calendar)
			sb.append (dateformat.format(((Calendar)obj).getTime()));
		else if (obj instanceof byte[])
			sb.append ( Base64.encodeBytes((byte[]) obj, Base64.DONT_BREAK_LINES));
		else
		{
			sb.append ('\n');
			Class cl = obj.getClass ();
			while (cl != null)
			{
				for (Field f: cl.getDeclaredFields())
				{
					String getter;
					if (f.getType() == boolean.class)
						getter = "is"+f.getName().substring(0,1).toUpperCase()+f.getName().substring(1);
					else
						getter = "get"+f.getName().substring(0,1).toUpperCase()+f.getName().substring(1);
					try {
						Method m = obj.getClass().getMethod(getter, null);
						Object result = m.invoke(obj);
						serialize (sb, indent+2,f.getName(), result, true);
					} catch (Throwable e)
					{
					}
				}
				cl = cl.getSuperclass();
			}
			if (closeTag)
			{
				for (int i = 0; i < indent; i++)
					sb.append (' ');
			}
		}
		if (closeTag)
			sb.append ("</").append(tag).append(">\n");
		else
			sb.append ("\n");
	}

	private Object unscape(String string) {
		return string.replaceAll("&", "&amp;")
				.replaceAll("<", "&lt;")
				.replaceAll(">", "&gt;");
	}

	@Override
	protected void handleRestoreBackup(UserBackup backup) throws Exception {
		UserBackupEntity backupEntity = getUserBackupEntityDao().load(backup.getId());
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(new ByteArrayInputStream (backupEntity.getData().getBytes("UTF-8")));
		
		Element update= doc.getDocumentElement();
		if (!update.getTagName().equals(USER_BACKUP))
			throw new InternalErrorException (String.format("Unexpected tag %s", update.getTagName()));
		User user = new User();
		List<UserAccount> accounts = new LinkedList<UserAccount>();
		List<RoleAccount> roleGrants = new LinkedList<RoleAccount>();
		List<String> groups = new LinkedList<String>();
		List<UserData> dades = new LinkedList<UserData>();
		
		NodeList children = update.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if (child instanceof Element)
			{
				String tag = ((Element) child).getTagName();
				if (tag.equals (USER))
					parseUser (child, user);
				else if (tag.equals (ACCOUNT))
				{
					UserAccount account = new UserAccount();
					account.setUser(user.getUserName());
					parseAccount (child, account, roleGrants);
					accounts.add(account);
				}
				else if (tag.equals (GROUP_MEMBERSHIP))
					groups.add (child.getTextContent());
				else if (tag.equals (CUSTOM_DATA))
				{
					UserData dada = new UserData ();
					parseDada (child, dada);
					dades.add (dada);
				}
				else
				{
					throw new InternalErrorException (String.format("Unexpected tag %s", tag));
				}
			}
		}
		
		Security.nestedLogin(Security.getCurrentAccount(), Security.ALL_PERMISSIONS);
		try {
			user = restoreUser(user);
			restoreDades (user, dades);
			restoreAccounts (user, accounts);	
			restoreGroups (user, groups);
			restoreRoles (user, accounts, roleGrants);
		} finally {
			Security.nestedLogoff();
		}
	}
	
	private boolean nullCompare( Object o1, Object o2)
	{
		if (o1 != null && o1.equals ("")) o1 = null;
		if (o2 != null && o2.equals ("")) o2 = null;
		
		if (o1 == null && o2 == null)
			return true;
		else if (o1 == null || o2 == null)
			return false;
		else
			return o1.equals(o2);
	}

	private void restoreRoles(User User, List<UserAccount> accounts, List<RoleAccount> roleGrants) throws InternalErrorException {
		LinkedList<RoleAccount> currentGrants = new LinkedList<RoleAccount>();
		for (UserAccount account:accounts)
		{
			currentGrants.addAll ( getApplicationService().findRoleAccountByAccount(account.getId()));
		}
		//
		// Update existing roleUser
		//
		for (Iterator<RoleAccount> it = currentGrants.iterator(); it.hasNext();)
		{
			RoleAccount currentRoleAccount = it.next();
			String currentDomainValue = null;
			if (currentRoleAccount.getDomainValue() != null)
				currentDomainValue = currentRoleAccount.getDomainValue().getValue();
			// Find data on backup
			for (Iterator<RoleAccount> it2 = roleGrants.iterator(); it2.hasNext();)
			{
				RoleAccount RoleAccount = it2.next();
				String domainValue = null;
				if (RoleAccount.getDomainValue() != null)
					domainValue = RoleAccount.getDomainValue().getValue();
				if (currentRoleAccount.getAccountName().equals( RoleAccount.getAccountName()) &&
						currentRoleAccount.getRoleName().equals(RoleAccount.getRoleName()) &&
						currentRoleAccount.getSystem().equals (RoleAccount.getSystem()) &&
						currentRoleAccount.getAccountSystem().equals(RoleAccount.getAccountSystem()) &&
						nullCompare (domainValue, currentDomainValue) )
				{
					it2.remove();
					it.remove();
					break;
				}
			}
		}
			
		//
		// Remove old RoleAccount
		//
		for (Iterator<RoleAccount> it = currentGrants.iterator(); it.hasNext();)
		{
			RoleAccount currentRoleAccount = it.next();
			getApplicationService().delete(currentRoleAccount);
		}
		// Create new role accounts
		for (Iterator<RoleAccount> it2 = roleGrants.iterator(); it2.hasNext();)
		{
			RoleAccount RoleAccount = it2.next();
			Collection<Role> roles = getApplicationService().
					findRolesByFilter(RoleAccount.getRoleName(), "%", "%", RoleAccount.getSystem(), "%", "%");
			if (roles.size() != 1)
				throw new InternalErrorException (
						String.format("Role %s@%s not found", 
								RoleAccount.getRoleName(), RoleAccount.getSystem()));
			Role rol = roles.iterator().next();

			if (RoleAccount.getDomainValue() != null)
				RoleAccount.getDomainValue().setDomainName(rol.getDomain().getName());
			for (UserAccount account:accounts)
			{
				if (account.getName().equals (RoleAccount.getAccountName()) &&
						account.getSystem().equals(RoleAccount.getAccountSystem()))
					RoleAccount.setAccountId(account.getId());
			}
			RoleAccount.setInformationSystemName(rol.getInformationSystemName());
			getApplicationService().create(RoleAccount);
		}			
	}

	private void restoreGroups(User user, List<String> groups) throws InternalErrorException {
		Collection<GroupUser> currentGroups = getGroupService().findUsersGroupByUserName(user.getUserName());
		//
		// Update existing accodataunts
		//
		for (Iterator<GroupUser> it = currentGroups.iterator(); it.hasNext();)
		{
			GroupUser currentGroup = it.next();
			// Find data on backup
			for (Iterator<String> it2 = groups.iterator(); it2.hasNext();)
			{
				String group = it2.next();
				if (currentGroup.getGroup().equals (group))
				{
					it2.remove();
					it.remove();
					break;
				}
			}
		}
			
		//
		// Remove old accounts
		//
		for (Iterator<GroupUser> it = currentGroups.iterator(); it.hasNext();)
		{
			GroupUser currentGroup = it.next();
			// Find account on backup
			getGroupService().delete(currentGroup);
		}
		// Create new group
		for (Iterator<String> it2 = groups.iterator(); it2.hasNext();)
		{
			String group = it2.next();
			getGroupService().addGroupToUser(user.getUserName(), group);
		}			
	}

	private void restoreDades(User user, List<UserData> dades) throws InternalErrorException {
		Collection<UserData> currentDatas = getUserService().findUserDataByUserName(user.getUserName());
		//
		// Update existing accodataunts
		//
		for (Iterator<UserData> it = currentDatas.iterator(); it.hasNext();)
		{
			UserData currentData = it.next();
			// Find data on backup
			for (Iterator<UserData> it2 = dades.iterator(); it2.hasNext();)
			{
				UserData dada = it2.next();
				if (currentData.getAttribute().equals (dada.getAttribute()))
				{
					currentData.setValue(dada.getValue());
					getAdditionalDataService().update(currentData);
					it2.remove();
					it.remove();
					break;
				}
			}
		}
			
		//
		// Remove old accounts
		//
		for (Iterator<UserData> it = currentDatas.iterator(); it.hasNext();)
		{
			UserData currentData = it.next();
			getAdditionalDataService().delete(currentData);
		}
		// Create new accounts
		for (Iterator<UserData> it2 = dades.iterator(); it2.hasNext();)
		{
			UserData dada = it2.next();
			dada.setUser(user.getUserName());
			getAdditionalDataService().create(dada);
		}			
			
	}

	private void restoreAccounts(User user, List<UserAccount> accounts) throws InternalErrorException, AccountAlreadyExistsException, NeedsAccountNameException {
		accounts = new LinkedList<UserAccount>(accounts);
		Collection<UserAccount> currentAccounts = getAccountService().getUserAccounts(user);
		//
		// Update existing accounts
		//
		for (Iterator<UserAccount> it = currentAccounts.iterator(); it.hasNext();)
		{
			UserAccount currentAccount = it.next();
			// Find account on backup
			for (Iterator<UserAccount> it2 = accounts.iterator(); it2.hasNext();)
			{
				UserAccount account = it2.next();
				if (currentAccount.getName().equals (account.getName()) &&
						currentAccount.getSystem().equals(account.getSystem()))
				{
					if (currentAccount.isDisabled() != account.isDisabled())
					{
						currentAccount.setDisabled(account.isDisabled());
						getAccountService().updateAccount(currentAccount);
					}
					account.setId(currentAccount.getId());
					it2.remove();
					it.remove();
					break;
				}
			}
		}
			
		//
		// Remove old accounts
		//
		for (Iterator<UserAccount> it = currentAccounts.iterator(); it.hasNext();)
		{
			UserAccount currentAccount = it.next();
			// Find account on backup
			getAccountService().removeAccount(currentAccount);
		}
		// Create new accounts
		for (Iterator<UserAccount> it2 = accounts.iterator(); it2.hasNext();)
		{
			UserAccount account = it2.next();
			System system = getDispatcherService().findDispatcherByName(account.getDescription());
			UserAccount newAccount = getAccountService().createAccount(user, system, account.getName());
			account.setId(newAccount.getId());
		}			
			
	}

	private User restoreUser(User user) throws InternalErrorException {
		User currentUser = getUserService().findUserByUserName(user.getUserName());
		if (currentUser == null)
		{
			user.setId(null);
			user = getUserService().create(user);
		}
		else
		{
			user.setId(currentUser.getId());
			user = getUserService().update(user);
		}
		return user;
	}

	private void parseDada(Node dadaElement, UserData dada) throws InternalErrorException {
		NodeList children = dadaElement.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if (child instanceof Element)
			{
				String tag = ((Element) child).getTagName();
				if (TAG.equals(tag))
					dada.setAttribute(child.getTextContent());
				else if (VALUE.equals(tag))
					dada.setValue(child.getTextContent());
				else
					throw new InternalErrorException (String.format("Unexpected tag %s", tag));
			}
		}
	}

	private void parseUser(Node userTag, User user) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, DOMException, ParseException {
		NodeList children = userTag.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if (child instanceof Element)
			{
				Element element = (Element) child;
				parseAttribute(user, element);
			}
		}
	}

	private void parseAccount(Node accountElement, UserAccount account, List<RoleAccount> roleGrants) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, DOMException, ParseException, InternalErrorException {
		NodeList children = accountElement.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if (child instanceof Element)
			{
				String tag = ((Element) child).getTagName();
				if (NAME.equals(tag))
					account.setName(child.getTextContent());
				else if (SYSTEM.equals(tag))
					account.setSystem(child.getTextContent());
				else if (ROLE.equals(tag))
				{
					RoleAccount roleAccount = new RoleAccount();
					roleGrants.add(roleAccount);
					roleAccount.setAccountName(account.getName());
					roleAccount.setAccountSystem(account.getSystem());
					parseRoleAccount (child, roleAccount);
				}
				else
					throw new InternalErrorException (String.format("Unexpected tag %s", tag));
			}
		}
	}

	private void parseRoleAccount(Node roleElement, RoleAccount roleAccount) throws InternalErrorException {
		NodeList children = roleElement.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if (child instanceof Element)
			{
				String tag = ((Element) child).getTagName();
				if (NAME.equals(tag))
					roleAccount.setRoleName(child.getTextContent());
				else if (SYSTEM.equals(tag))
					roleAccount.setSystem(child.getTextContent());
				else if (DOMAIN.equals(tag))
				{
					roleAccount.setDomainValue(new DomainValue());
					roleAccount.getDomainValue().setValue(child.getTextContent());
				}
				else
					throw new InternalErrorException (String.format("Unexpected tag %s", tag));
			}
		}
	}

	private void parseAttribute(Object object, Element element)
			throws NoSuchFieldException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException, ParseException {
		Field f = null;
		Class cl = object.getClass ();
		while (cl != null)
		{
			try
			{
				f = cl.getDeclaredField(element.getTagName());
				if (f != null)
					break;
			} catch (NoSuchFieldException e) {}
			cl = cl.getSuperclass();
		}
		if (f == null)
			throw new NoSuchFieldException(element.getTagName());
		Method m = object.getClass().getMethod(
				"set"+f.getName().substring(0,1).toUpperCase()+
						f.getName().substring(1),
				f.getType());
		if (! element.hasChildNodes())
			m.invoke(object, new Object[] {null});
		else if (f.getType().isAssignableFrom(String.class))
		{
			m.invoke(object, element.getTextContent());
		}
		else if (f.getType().isAssignableFrom(Boolean.class))
		{
			m.invoke(object, Boolean.valueOf(element.getTextContent()));
		}
		else if (f.getType().isAssignableFrom(Integer.class))
		{
			m.invoke(object, Integer.valueOf(element.getTextContent()));
		}
		else if (f.getType().isAssignableFrom(Long.class))
		{
			m.invoke(object, Long.valueOf(element.getTextContent()));
		}
		else if (f.getType().isAssignableFrom(Date.class))
		{
			m.invoke(object, dateformat.parse(element.getTextContent()));
		}
		else if (f.getType().isAssignableFrom(Calendar.class))
		{
			Calendar c = Calendar.getInstance();
			c.setTime(dateformat.parse(element.getTextContent()));
			m.invoke(object, c);
		}
		else if (f.getType().isAssignableFrom(byte[].class))
		{
			m.invoke(object, Base64.decode(element.getTextContent()));
		}
	}

	@Override
	protected List<UserBackup> handleFindBackup(String user, Date date,
			String freeText) throws Exception {
		if (user != null && user.length() == 0)
			user = null;
		if (freeText != null && freeText.length() == 0)
			freeText = null;
		List<UserBackupEntity> entities = getUserBackupEntityDao().findByCriteria(user, date);
		LinkedList<UserBackup> result = new LinkedList<UserBackup>();
		if (freeText != null)
			freeText = freeText.replaceAll ("%", "").replaceAll("\\*", "");
		for (UserBackupEntity backup: entities)
		{
			if (backup.getValidUntil() != null)
			{
				if (freeText == null || backup.getData().contains(freeText))
					result.add(getUserBackupEntityDao().toUserBackup(backup));
			}
		}
		return result;
	}

	@Override
	protected List<UserBackup> handleGetUserBackups(Long userId)
			throws Exception {
		UserEntity user = getUserEntityDao().load(userId);
		if (user == null)
			Collections.emptyList();
		List<UserBackupEntity> entities = getUserBackupEntityDao().findByUser(user.getUserName());
		for (Iterator<UserBackupEntity> it = entities.iterator(); it.hasNext();)
		{
			if (it.next().getValidUntil() == null)
				it.remove();
		}
		Collections.sort(entities, new Comparator<UserBackupEntity>() {

			public int compare(UserBackupEntity o1, UserBackupEntity o2) {
				return - (o2.getOrder().compareTo(o1.getOrder()));
			}
		});

		return getUserBackupEntityDao().toUserBackupList(entities);
	}

	
	@Override
	protected UserBackupConfig handleGetConfig() throws Exception {
		UserBackupConfig cfg = new UserBackupConfig();
		cfg.setCmdToExecute(ConfigurationCache.getProperty("addon.backup.cmd"));
		cfg.setFullBackupDir(ConfigurationCache.getProperty("addon.backup.fullBackupDir"));
		try {
			cfg.setFullBackupHour(Integer.decode(ConfigurationCache.getProperty("addon.backup.hour")));
		} catch (Exception e) {}
		
		if (cfg.getFullBackupHour() == null)
			cfg.setFullBackupHour(0);
		
		try {
			cfg.setFullBackupMinute(Integer.decode(ConfigurationCache.getProperty("addon.backup.minute")));
		} catch (Exception e) {}
		if (cfg.getFullBackupMinute() == null)
			cfg.setFullBackupMinute(0);
		
		try {
			cfg.setUserBackupCopies(Integer.decode(ConfigurationCache.getProperty("addon.backup.copies")));
		} catch (Exception e) {}
		if (cfg.getUserBackupCopies() == null)
			cfg.setUserBackupCopies(10);
		
		try {
			cfg.setUserBackupDelay(Long.decode(ConfigurationCache.getProperty("addon.backup.delay")));
		} catch (Exception e) {}
		if (cfg.getUserBackupDelay() == null)
			cfg.setUserBackupDelay(300L);
		return cfg;
	}

	@Override
	protected void handleSetConfig(UserBackupConfig config) throws Exception {
		updateConfig ("addon.backup.cmd", config.getCmdToExecute());
		updateConfig ("addon.backup.fullBackupDir", config.getFullBackupDir());
		updateConfig ("addon.backup.hour", config.getFullBackupHour());
		updateConfig ("addon.backup.minute", config.getFullBackupMinute());
		updateConfig ("addon.backup.copies", config.getUserBackupCopies());
		updateConfig ("addon.backup.delay", config.getUserBackupDelay());
	}
	

	private void updateConfig (String tag, Object value) throws InternalErrorException
	{
		String sValue = value == null ? null: value.toString();
		com.soffid.iam.service.ConfigurationService cfgSvc = getConfigurationService();
		Configuration v = cfgSvc.findParameterByNameAndNetworkName(tag, null);
		if (v != null)
		{
			if (sValue == null)
				cfgSvc.delete(v);
			else
			{
				v.setValue(sValue);
				cfgSvc.update(v);
			}
		}
		else if (sValue != null)
		{
			v = new Configuration();
			v.setCode(tag);
			v.setValue(sValue);
			v.setDescription("Backup addon parameter");
			cfgSvc.create(v);
		}
		
	}
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	protected String handleGetBackupContent(UserBackup backup) throws Exception {
		UserBackupEntity backupEntity = getUserBackupEntityDao().load(backup.getId());
		if (backupEntity == null)
			return null;
		else
			return backupEntity.getData();
	}

}
