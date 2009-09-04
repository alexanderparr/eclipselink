/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 *     06/16/2009-2.0 Guy Pelletier 
 *       - 277039: JPA 2.0 Cache Usage Settings
 ******************************************************************************/
package org.eclipse.persistence.internal.jpa.deployment;

import javax.persistence.*;
import javax.persistence.spi.*;
import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Internal implementation of the PersistenceUnitInfo detailed in the EJB 3.0 specification
 * Used by our Java SE implementation so common method calls can be used in setting
 * of Container and non-Container EntityManagerFactories.
 */
public class SEPersistenceUnitInfo implements javax.persistence.spi.PersistenceUnitInfo {

    // What about 2.0 in 1.0 container here ...
    protected CachingType caching;
    protected String persistenceUnitName;
    protected String persistenceProviderClassName;
    protected DataSource jtaDataSource;
    protected DataSource nonJtaDataSource;
    protected PersistenceUnitTransactionType persistenceUnitTransactionType;
    protected List<String> mappingFiles;

    // names of jars specified in persistence.xml. they are later on used
    // to build jar-file URL.
    private Collection<String> jarFiles = new ArrayList<String>();
    protected List<URL> jarFileUrls;
    protected List<String> managedClassNames;
    protected URL persistenceUnitRootUrl;
    protected boolean excludeUnlistedClasses = true;
    
    // Persistence.xml loaded from the canonical model processor will 
    // populate the properties into this collection.
    protected List<SEPersistenceUnitProperty> persistenceUnitProperties = new ArrayList<SEPersistenceUnitProperty>();
    // Persistence.xml loaded from the metadata processor will populate the
    // properties into this properties map.
    protected Properties properties;
    
    protected ClassLoader tempClassLoader;
    protected ClassLoader realClassLoader;

    public SEPersistenceUnitInfo(){
        mappingFiles = new ArrayList<String>();
        managedClassNames = new ArrayList<String>();
        properties = new Properties();
        persistenceUnitTransactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
        // don't initialize jarFileUrls as it is lazily initialized
    }

    /**
    * @return The name of the persistence unit.
    * Corresponds to the <name> element in the persistence.xml file.
    */
    public String getPersistenceUnitName(){
        return persistenceUnitName;
    }
    
    public void setPersistenceUnitName(String persistenceUnitName){
        this.persistenceUnitName = persistenceUnitName;
    }
    
    /**
     * Used with the OX mapping file for the Canonical model processor.
     */
    public List<SEPersistenceUnitProperty> getPersistenceUnitProperties() {
       return persistenceUnitProperties; 
    }

    /**
     * Used with the OX mapping file for the Canonical model processor.
     */
    public void setPersistenceUnitProperties(List<SEPersistenceUnitProperty> persistenceUnitProperties) {
       this.persistenceUnitProperties = persistenceUnitProperties; 
    }

    /**
    * @return The fully qualified name of the persistence provider
    * implementation class.
    * Corresponds to the <provider> element in the persistence.xml
    * file.
    */
    public String getPersistenceProviderClassName(){
        return persistenceProviderClassName;
    }
    
    public void setPersistenceProviderClassName(String persistenceProviderClassName){
        this.persistenceProviderClassName = persistenceProviderClassName;
    }
    
    /**
    * @return The transaction type of the entity managers created
    * by the EntityManagerFactory.
    * The transaction type corresponds to the transaction-type
    * attribute in the persistence.xml file.
    */
    public PersistenceUnitTransactionType getTransactionType(){
        return persistenceUnitTransactionType;
    }

    public void setTransactionType(PersistenceUnitTransactionType persistenceUnitTransactionType){
        this.persistenceUnitTransactionType = persistenceUnitTransactionType;
    }
    
    /**
    * @return the JTA-enabled data source to be used by the
    * persistence provider.
    * The data source corresponds to the <jta-data-source>
    * element in the persistence.xml file or is provided at
    * deployment or by the container.
    */
    public DataSource getJtaDataSource(){
        return jtaDataSource;
    }
    
    public void setJtaDataSource(DataSource jtaDataSource){
        this.jtaDataSource = jtaDataSource;
    }
    
    /**
    * @return The non-JTA-enabled data source to be used by the
    * persistence provider for accessing data outside a JTA
    * transaction.
    * The data source corresponds to the named <non-jta-data-source>
    * element in the persistence.xml file or provided at
    * deployment or by the container.
    */
    public DataSource getNonJtaDataSource(){
        return nonJtaDataSource;
    }

    public void setNonJtaDataSource(DataSource nonJtaDataSource){
        this.nonJtaDataSource = nonJtaDataSource;
    }

    /**
    * @return The list of mapping file names that the persistence
    * provider must load to determine the mappings for the entity
    * classes. The mapping files must be in the standard XML
    * mapping format, be uniquely named and be resource-loadable
    * from the application classpath. This list will not include
    * the orm.xml file if one was specified.
    * Each mapping file name corresponds to a <mapping-file>
    * element in the persistence.xml file.
    */
    public List<String> getMappingFileNames(){
        return mappingFiles;
    }
    
    public void setMappingFileNames(List<String> mappingFiles){
        this.mappingFiles = mappingFiles;
    }
    /**
    * @return The list of JAR file URLs that the persistence
    * provider must examine for managed classes of the persistence
    * unit. Each jar file URL corresponds to a named <jar-file>
    * element in the persistence.xml file.
    */
    public List<URL> getJarFileUrls(){
        if (jarFileUrls == null) { // lazy initialization
            List<URL> jarFileUrls = new ArrayList<URL>(jarFiles.size());
            for (String jarFile : jarFiles) {
                try {
                    // build a URL relative to the PU Root
                    URL jarFileURL = new URL(persistenceUnitRootUrl, jarFile);
                    jarFileUrls.add(jarFileURL);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
            synchronized(this) {
                this.jarFileUrls = jarFileUrls;
            }
        }
        return Collections.unmodifiableList(jarFileUrls);
    }
    
    public void setJarFileUrls(List<URL> jarFileUrls){
        this.jarFileUrls = jarFileUrls;
    }
    
    /**
    * @return The URL for the jar file that is the root of the
    * persistence unit. If the persistence unit is rooted in
    * the WEB-INF/classes directory, this will be the URL of
    * that directory.
    */
    public URL getPersistenceUnitRootUrl(){
        return persistenceUnitRootUrl;
    }
    
    public void setPersistenceUnitRootUrl(URL persistenceUnitRootUrl){
        this.persistenceUnitRootUrl = persistenceUnitRootUrl;
    }
    
    /**
    * @return The list of the names of the classes that the
    * persistence provider must add it to its set of managed
    * classes. Each name corresponds to a named <class> element
    * in the persistence.xml file.
    */
    public List<String> getManagedClassNames(){
        return managedClassNames;
    }

    public void setManagedClassNames(List<String> managedClassNames){
        this.managedClassNames = managedClassNames;
    }
    /**
    * @return Whether classes in the root of the persistence
    * unit that have not been explicitly listed are to be
    * included in the set of managed classes.
    * This value corresponds to the <exclude-unlisted-classes>
    * element in the persistence.xml file.
    */
    public boolean excludeUnlistedClasses(){
        return excludeUnlistedClasses; 
    }
    
    public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses){
        this.excludeUnlistedClasses = excludeUnlistedClasses;
    }
    /**
    * @return Properties object. Each property corresponds
    * to a <property> element in the persistence.xml file
    */
    public Properties getProperties(){
        return properties;
    }

    public void setProperties(Properties properties){
        this.properties = properties;
    }
    /**
    * @return ClassLoader that the provider may use to load any
    * classes, resources, or open URLs.
    */
    public ClassLoader getClassLoader(){
        return realClassLoader;
    }
    /**
    * Add a transformer supplied by the provider that will be
    * called for every new class definition or class redefinition
    * that gets loaded by the loader returned by the
    * PersistenceUnitInfo.getClassLoader method. The transformer
    * has no effect on the result returned by the
    * PersistenceUnitInfo.getNewTempClassLoader method.
    * Classes are only transformed once within the same classloading
    * scope, regardless of how many persistence units they may be
    * a part of.
    *
    * @param transformer A provider-supplied transformer that the
    * Container invokes at class-(re)definition time
    */
    public void addTransformer(ClassTransformer transformer){
        // not required for our Java SE implementation
    }

    /**
    * Return a ClassLoader that the provider may use to temporarily
    * load any classes, resources, or open URLs. The scope and
    * classpath of this loader is exactly the same as that of the
    * loader returned by PersistenceUnitInfo.getClassLoader. None of the
    * classes loaded by this class loader will be visible to
    * application components.
    *
    * @return Temporary ClassLoader with same visibility as current
    * loader
    */
    public ClassLoader getNewTempClassLoader(){
        return tempClassLoader;
    }
    
    public void setNewTempClassLoader(ClassLoader loader){
        this.tempClassLoader = loader;
    }
    
    /**
     * @see PersistenceUnitInfo#setCaching()
     * @since Java Persistence 2.0
     */
    public void setCaching(String cachingType) {
        // If user enters in invalid caching type valueOf will throw an illegal
        // argument exception, e.g. 
        // java.lang.IllegalArgumentException: No enum const class javax.persistence.CachingType.ALLBOGUS
        this.caching = CachingType.valueOf(cachingType);
    }
    
    public void setClassLoader(ClassLoader loader) {
        this.realClassLoader = loader;
    }

    public Collection<String> getJarFiles() {
        return jarFiles;
    }

    /**
     * @see PersistenceUnitInfo#PersistenceXMLSchemaVersion()
     * @since Java Persistence 2.0
     */
    public String PersistenceXMLSchemaVersion() {
        // TODO 
        throw new PersistenceException("Not Yet Implemented");
    }

    /**
     * @see PersistenceUnitInfo#getCaching()
     * @since Java Persistence 2.0
     */
    public CachingType getCaching() {
        return caching;
    }
    
    /**
     * @see PersistenceUnitInfo#getValidationMode()
     * @since Java Persistence 2.0
     */
    public ValidationMode getValidationMode() {
        // TODO 
        throw new PersistenceException("Not Yet Implemented");
    }
}
