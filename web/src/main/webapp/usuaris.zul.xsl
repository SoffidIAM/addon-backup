<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:zul="http://www.zkoss.org/2005/zul">

	<xsl:output method="xml" omit-xml-declaration="no" indent="yes"/>

	<xsl:template match="datamodel[@rootNode='usuaris']" priority="3">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
		<zscript>
				canQueryBackup = es.caib.seycon.ng.utils.Security.isUserInRole("backup:query") || 
								  es.caib.seycon.ng.utils.Security.isUserInRole("backup:query/*");
				canRestoreBackup = es.caib.seycon.ng.utils.Security.isUserInRole("backup:restore") || 
								  es.caib.seycon.ng.utils.Security.isUserInRole("backup:restore/*");
				model.getVariables().declareVariable("canQueryBackup", canQueryBackup);
		</zscript>
	</xsl:template>
 
	<xsl:template match="tabbox[@id='panels']/tabs" priority="3">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
			<tab  visible="${{canQueryBackup}}" label="${{c:l('backup.backups')}}">
			</tab>
		</xsl:copy>
	</xsl:template>
 

	<xsl:template match="tabbox[@id='panels']/tabpanels" priority="3">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
			<tabpanel id="backups">
 				<backup_tab/>
			</tabpanel>						
		</xsl:copy>
	</xsl:template>


	<xsl:template match="/" priority="3">
		<xsl:processing-instruction name="component">name="backup_tab" macro-uri="/addon/backup/userbackup.zul"</xsl:processing-instruction>
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
	</xsl:template>


	<xsl:template match="node()|@*" priority="2">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
	</xsl:template>


</xsl:stylesheet>