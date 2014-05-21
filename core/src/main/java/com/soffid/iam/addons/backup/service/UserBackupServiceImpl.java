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
import com.soffid.iam.api.RoleAccount;
import com.soffid.iam.api.User;

import es.caib.seycon.ng.comu.Configuracio;
import es.caib.seycon.ng.comu.DadaUsuari;
import es.caib.seycon.ng.comu.Dispatcher;
import es.caib.seycon.ng.comu.Grup;
import es.caib.seycon.ng.comu.Rol;
import es.caib.seycon.ng.comu.RolAccount;
import es.caib.seycon.ng.comu.UserAccount;
import es.caib.seycon.ng.comu.Usuari;
import es.caib.seycon.ng.comu.UsuariGrup;
import es.caib.seycon.ng.comu.ValorDomini;
import es.caib.seycon.ng.exception.AccountAlreadyExistsException;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.seycon.ng.exception.NeedsAccountNameException;
import es.caib.seycon.ng.model.UsuariEntity;
import es.caib.seycon.ng.servei.ConfiguracioService;
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
		UsuariEntity user = getUsuariEntityDao().findByCodi(userId);
		if (user == null)
			return;
		
		Security.nestedLogin(Security.getCurrentAccount(), new String [] { Security.AUTO_AUTHORIZATION_ALL });
		String xml;
		try{
			xml = generateXml (user);
		} finally {
			Security.nestedLogoff();
		}
		long order = 0;
		Date now = new Date();
		List<UserBackupEntity> oldBackups = getUserBackupEntityDao().findByUser(user.getCodi());
		
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
		backup.setUserName(user.getCodi());
		getUserBackupEntityDao().create(backup);
	}

	private String generateXml(UsuariEntity userEntity) throws InternalErrorException {
		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
			.append("\n");
		
		serialize (sb, 0, USER_BACKUP, null, false);
		Usuari usuari = getUsuariEntityDao().toUsuari(userEntity);
		User user = User.toUser( usuari );
		
		serialize (sb, 2, USER, user, true);
		
		for (UserAccount account: getAccountService().getUserAccounts(usuari))
		{
			if (!account.isDisabled())
			{
				serialize (sb, 2, ACCOUNT, null, false);
				serialize (sb, 4, NAME, account.getName(), true);
				serialize (sb, 4, SYSTEM, account.getDispatcher(), true);
				for (RolAccount rolAccount: getAplicacioService().findRolAccountByAccount(account.getId()))
				{
					serialize (sb, 4, ROLE, null, false);
					serialize (sb, 6, NAME, rolAccount.getNomRol(), true);
					serialize (sb, 6, SYSTEM, rolAccount.getBaseDeDades(), true);
					if (rolAccount.getValorDomini() != null)
						serialize (sb, 6, DOMAIN, rolAccount.getValorDomini().getValor(), true);
					closeTag (sb, 4, ROLE);
				}
				closeTag (sb, 2, ACCOUNT);
			}
		}
		
		for (UsuariGrup usuariGrup: getGrupService().findUsuariGrupsByCodiUsuari(userEntity.getCodi()))
		{
			serialize (sb, 2, GROUP_MEMBERSHIP, usuariGrup.getCodiGrup(), true);
		}
		
		for (DadaUsuari dada: getUsuariService().findDadesUsuariByCodiUsuari(userEntity.getCodi()))
		{
			if (dada.getValorDada() != null)
			{
				serialize (sb, 2, CUSTOM_DATA, null, false);
				serialize (sb, 4, TAG, dada.getCodiDada(), true);
				serialize (sb, 4, VALUE, dada.getValorDada(), true);
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
		List<RolAccount> roleGrants = new LinkedList<RolAccount>();
		List<String> groups = new LinkedList<String>();
		List<DadaUsuari> dades = new LinkedList<DadaUsuari>();
		
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
					DadaUsuari dada = new DadaUsuari ();
					parseDada (child, dada);
					dades.add (dada);
				}
				else
				{
					throw new InternalErrorException (String.format("Unexpected tag %s", tag));
				}
			}
		}
		
		Security.nestedLogin(Security.getCurrentAccount(), new String[] { 
				Security.AUTO_AUTHORIZATION_ALL });
		try {
			Usuari usuari = Usuari.toUsuari(user);
			usuari = restoreUsuari(usuari);
			restoreDades (usuari, dades);
			restoreAccounts (usuari, accounts);	
			restoreGroups (usuari, groups);
			restoreRoles (usuari, accounts, roleGrants);
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

	private void restoreRoles(Usuari usuari, List<UserAccount> accounts, List<RolAccount> roleGrants) throws InternalErrorException {
		LinkedList<RolAccount> currentGrants = new LinkedList<RolAccount>();
		for (UserAccount account:accounts)
		{
			currentGrants.addAll ( getAplicacioService().findRolAccountByAccount(account.getId()));
		}
		//
		// Update existing roleUsuari
		//
		for (Iterator<RolAccount> it = currentGrants.iterator(); it.hasNext();)
		{
			RolAccount currentRolAccount = it.next();
			String currentDomainValue = null;
			if (currentRolAccount.getValorDomini() != null)
				currentDomainValue = currentRolAccount.getValorDomini().getValor();
			// Find data on backup
			for (Iterator<RolAccount> it2 = roleGrants.iterator(); it2.hasNext();)
			{
				RolAccount rolAccount = it2.next();
				String domainValue = null;
				if (rolAccount.getValorDomini() != null)
					domainValue = rolAccount.getValorDomini().getValor();
				if (currentRolAccount.getAccountName().equals( rolAccount.getAccountName()) &&
						currentRolAccount.getNomRol().equals(rolAccount.getNomRol()) &&
						currentRolAccount.getBaseDeDades().equals (rolAccount.getBaseDeDades()) &&
						currentRolAccount.getAccountDispatcher().equals(rolAccount.getAccountDispatcher()) &&
						nullCompare (domainValue, currentDomainValue) )
				{
					it2.remove();
					it.remove();
					break;
				}
			}
		}
			
		//
		// Remove old rolAccount
		//
		for (Iterator<RolAccount> it = currentGrants.iterator(); it.hasNext();)
		{
			RolAccount currentRolAccount = it.next();
			getAplicacioService().delete(currentRolAccount);
		}
		// Create new role accounts
		for (Iterator<RolAccount> it2 = roleGrants.iterator(); it2.hasNext();)
		{
			RolAccount rolAccount = it2.next();
			Collection<Rol> roles = getAplicacioService().
					findRolsByFiltre(rolAccount.getNomRol(), "%", "%", rolAccount.getBaseDeDades(), "%", "%");
			if (roles.size() != 1)
				throw new InternalErrorException (
						String.format("Role %s@%s not found", 
								rolAccount.getNomRol(), rolAccount.getBaseDeDades()));
			Rol rol = roles.iterator().next();

			if (rolAccount.getValorDomini() != null)
				rolAccount.getValorDomini().setNomDomini(rol.getDomini().getNom());
			for (UserAccount account:accounts)
			{
				if (account.getName().equals (rolAccount.getAccountName()) &&
						account.getDispatcher().equals(rolAccount.getAccountDispatcher()))
					rolAccount.setAccountId(account.getId());
			}
			rolAccount.setCodiAplicacio(rol.getCodiAplicacio());
			getAplicacioService().create(rolAccount);
		}			
	}

	private void restoreGroups(Usuari usuari, List<String> groups) throws InternalErrorException {
		Collection<UsuariGrup> currentGroups = getGrupService().findUsuariGrupsByCodiUsuari(usuari.getCodi());
		//
		// Update existing accodataunts
		//
		for (Iterator<UsuariGrup> it = currentGroups.iterator(); it.hasNext();)
		{
			UsuariGrup currentGroup = it.next();
			// Find data on backup
			for (Iterator<String> it2 = groups.iterator(); it2.hasNext();)
			{
				String group = it2.next();
				if (currentGroup.getCodiGrup().equals (group))
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
		for (Iterator<UsuariGrup> it = currentGroups.iterator(); it.hasNext();)
		{
			UsuariGrup currentGroup = it.next();
			// Find account on backup
			getGrupService().delete(currentGroup);
		}
		// Create new group
		for (Iterator<String> it2 = groups.iterator(); it2.hasNext();)
		{
			String group = it2.next();
			getGrupService().addGrupToUsuari(usuari.getCodi(), group);
		}			
	}

	private void restoreDades(Usuari usuari, List<DadaUsuari> dades) throws InternalErrorException {
		Collection<DadaUsuari> currentDatas = getUsuariService().findDadesUsuariByCodiUsuari(usuari.getCodi());
		//
		// Update existing accodataunts
		//
		for (Iterator<DadaUsuari> it = currentDatas.iterator(); it.hasNext();)
		{
			DadaUsuari currentData = it.next();
			// Find data on backup
			for (Iterator<DadaUsuari> it2 = dades.iterator(); it2.hasNext();)
			{
				DadaUsuari dada = it2.next();
				if (currentData.getCodiDada().equals (dada.getCodiDada()))
				{
					currentData.setValorDada(dada.getValorDada());
					getDadesAddicionalsService().update(currentData);
					it2.remove();
					it.remove();
					break;
				}
			}
		}
			
		//
		// Remove old accounts
		//
		for (Iterator<DadaUsuari> it = currentDatas.iterator(); it.hasNext();)
		{
			DadaUsuari currentData = it.next();
			getDadesAddicionalsService().delete(currentData);
		}
		// Create new accounts
		for (Iterator<DadaUsuari> it2 = dades.iterator(); it2.hasNext();)
		{
			DadaUsuari dada = it2.next();
			dada.setCodiDada(usuari.getCodi());
			getDadesAddicionalsService().create(dada);
		}			
			
	}

	private void restoreAccounts(Usuari usuari, List<UserAccount> accounts) throws InternalErrorException, AccountAlreadyExistsException, NeedsAccountNameException {
		accounts = new LinkedList<UserAccount>(accounts);
		Collection<UserAccount> currentAccounts = getAccountService().getUserAccounts(usuari);
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
						currentAccount.getDispatcher().equals(account.getDispatcher()))
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
			Dispatcher dispatcher = getDispatcherService().findDispatcherByCodi(account.getDescription());
			UserAccount newAccount = getAccountService().createAccount(usuari, dispatcher, account.getName());
			account.setId(newAccount.getId());
		}			
			
	}

	private Usuari restoreUsuari(Usuari usuari) throws InternalErrorException {
		Usuari currentUsuari = getUsuariService().findUsuariByCodiUsuari(usuari.getCodi());
		if (currentUsuari == null)
		{
			usuari.setId(null);
			usuari = getUsuariService().create(usuari);
		}
		else
		{
			usuari.setId(currentUsuari.getId());
			usuari = getUsuariService().update(usuari);
		}
		return usuari;
	}

	private void parseDada(Node dadaElement, DadaUsuari dada) throws InternalErrorException {
		NodeList children = dadaElement.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if (child instanceof Element)
			{
				String tag = ((Element) child).getTagName();
				if (TAG.equals(tag))
					dada.setCodiDada(child.getTextContent());
				else if (VALUE.equals(tag))
					dada.setValorDada(child.getTextContent());
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

	private void parseAccount(Node accountElement, UserAccount account, List<RolAccount> roleGrants) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, DOMException, ParseException, InternalErrorException {
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
					account.setDispatcher(child.getTextContent());
				else if (ROLE.equals(tag))
				{
					RolAccount roleAccount = new RolAccount();
					roleGrants.add(roleAccount);
					roleAccount.setAccountName(account.getName());
					roleAccount.setAccountDispatcher(account.getDispatcher());
					parseRoleAccount (child, roleAccount);
				}
				else
					throw new InternalErrorException (String.format("Unexpected tag %s", tag));
			}
		}
	}

	private void parseRoleAccount(Node roleElement, RolAccount roleAccount) throws InternalErrorException {
		NodeList children = roleElement.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if (child instanceof Element)
			{
				String tag = ((Element) child).getTagName();
				if (NAME.equals(tag))
					roleAccount.setNomRol(child.getTextContent());
				else if (SYSTEM.equals(tag))
					roleAccount.setBaseDeDades(child.getTextContent());
				else if (DOMAIN.equals(tag))
				{
					roleAccount.setValorDomini(new ValorDomini());
					roleAccount.getValorDomini().setValor(child.getTextContent());
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
		UsuariEntity user = getUsuariEntityDao().load(userId);
		if (user == null)
			Collections.emptyList();
		List<UserBackupEntity> entities = getUserBackupEntityDao().findByUser(user.getCodi());
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
		cfg.setCmdToExecute(System.getProperty("addon.backup.cmd"));
		cfg.setFullBackupDir(System.getProperty("addon.backup.fullBackupDir"));
		try {
			cfg.setFullBackupHour(Integer.decode(System.getProperty("addon.backup.hour")));
		} catch (Exception e) {}
		
		if (cfg.getFullBackupHour() == null)
			cfg.setFullBackupHour(0);
		
		try {
			cfg.setFullBackupMinute(Integer.decode(System.getProperty("addon.backup.minute")));
		} catch (Exception e) {}
		if (cfg.getFullBackupMinute() == null)
			cfg.setFullBackupMinute(0);
		
		try {
			cfg.setUserBackupCopies(Integer.decode(System.getProperty("addon.backup.copies")));
		} catch (Exception e) {}
		if (cfg.getUserBackupCopies() == null)
			cfg.setUserBackupCopies(10);
		
		try {
			cfg.setUserBackupDelay(Long.decode(System.getProperty("addon.backup.delay")));
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
		ConfiguracioService cfgSvc = getConfiguracioService();
		Configuracio v = cfgSvc.findParametreByCodiAndCodiXarxa(tag, null);
		if (v != null)
		{
			if (sValue == null)
				cfgSvc.delete(v);
			else
			{
				v.setValor(sValue);
				cfgSvc.update(v);
			}
		}
		else if (sValue != null)
		{
			v = new Configuracio();
			v.setCodi(tag);
			v.setValor(sValue);
			v.setDescripcio("Backup addon parameter");
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
