/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dynamia.modules.entityfile.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.dynamia.modules.entityfile.EntityFileAware;
import com.dynamia.modules.entityfile.EntityFileException;
import com.dynamia.modules.entityfile.FilesConfig;
import com.dynamia.modules.entityfile.UploadedFileInfo;
import com.dynamia.modules.entityfile.domain.EntityFile;
import com.dynamia.modules.entityfile.domain.enums.EntityFileState;
import com.dynamia.modules.entityfile.enums.EntityFileType;
import com.dynamia.modules.entityfile.service.EntityFileService;
import com.dynamia.modules.saas.AccountContext;
import com.dynamia.tools.commons.BeanUtils;
import com.dynamia.tools.commons.StringUtils;
import com.dynamia.tools.commons.logger.LoggingService;
import com.dynamia.tools.commons.logger.SLF4JLoggingService;
import com.dynamia.tools.domain.AbstractEntity;
import com.dynamia.tools.domain.query.Parameters;
import com.dynamia.tools.domain.query.QueryConditions;
import com.dynamia.tools.domain.query.QueryParameters;
import com.dynamia.tools.domain.services.CrudService;
import com.dynamia.tools.integration.Containers;
import com.dynamia.tools.integration.scheduling.Task;
import com.dynamia.tools.io.IOUtils;

/**
 *
 * @author djinn
 */
@Service
public class EntityFileServiceImpl implements EntityFileService {

	private LoggingService logger = new SLF4JLoggingService(EntityFileService.class);

	@Autowired
	private CrudService crudService;

	@Autowired
	private Parameters appParams;

	@Override
	public EntityFile createDirectory(EntityFile parent, String name, String description) {
		return createDir(parent, null, name, description);
	}

	@Override
	public EntityFile createDirectory(AbstractEntity entity, String name, String description) {
		return createDir(null, entity, name, description);
	}

	private EntityFile createDir(EntityFile parent, AbstractEntity targetEntity, String name, String descripcion) {
		EntityFile entityFile = new EntityFile();
		entityFile.setAccount(AccountContext.getCurrent().getAccount());
		entityFile.setParent(parent);
		entityFile.setName(name);
		entityFile.setState(EntityFileState.VALID);
		entityFile.setDescription(descripcion);
		if (targetEntity != null) {
			entityFile.setTargetEntity(targetEntity.getClass().getName());
			entityFile.setTargetEntityId((Long) targetEntity.getId());
		} else {
			entityFile.setTargetEntity(parent.getTargetEntity());
			entityFile.setTargetEntityId(parent.getTargetEntityId());
			entityFile.setTargetEntitySId(parent.getTargetEntitySId());
		}
		entityFile.setShared(true);
		entityFile.setType(EntityFileType.DIRECTORY);
		entityFile = crudService.save(entityFile);
		return entityFile;
	}

	@Override
	@Transactional
	public EntityFile createEntityFile(UploadedFileInfo fileInfo, AbstractEntity target, String description) {
		target = crudService.reload(target);
		logger.info("Creating new entity file for " + target + ", file: " + fileInfo.getFullName());
		EntityFile entityFile = new EntityFile();
		entityFile.setAccount(AccountContext.getCurrent().getAccount());
		entityFile.setDescription(description);
		entityFile.setContentType(fileInfo.getContentType());
		entityFile.setName(fileInfo.getFullName());
		entityFile.setExtension(StringUtils.getFilenameExtension(fileInfo.getFullName()));
		entityFile.setTargetEntity(target.getClass().getName());
		if (target.getId() instanceof Long) {
			entityFile.setTargetEntityId((Long) target.getId());
		} else {
			entityFile.setTargetEntitySId(target.getId().toString());
		}
		entityFile.setType(EntityFileType.FILE);
		entityFile.setParent(fileInfo.getParent());
		entityFile.setState(EntityFileState.VALID);
		crudService.create(entityFile);
		processEntityFileAware(target);

		File realFile = getRealFile(entityFile);
		try {
			realFile.getParentFile().mkdirs();
			realFile.createNewFile();
			realFile.setExecutable(false);

			IOUtils.copy(fileInfo.getInputStream(), realFile);
			System.out.println("File Saved: " + realFile.getName() + "  length: " + realFile.length());
			crudService.updateField(entityFile, "size", realFile.length());

		} catch (IOException e) {
			throw new EntityFileException(
					"Error writing file " + fileInfo.getFullName() + " on new location " + realFile.getAbsolutePath(), e);
		}

		return entityFile;
	}

	@Override
	@Transactional
	public EntityFile createEntityFile(UploadedFileInfo fileInfo, AbstractEntity target) {
		return createEntityFile(fileInfo, target, null);
	}

	@Override
	@Transactional
	public void delete(EntityFile entityFile) {
		crudService.updateField(entityFile, "state", EntityFileState.DELETED);
	}

	@Override
	public List<EntityFile> getEntityFiles(AbstractEntity entity) {
		return getEntityFiles(entity.getClass(), entity.getId(), null);
	}

	@Override
	public List<EntityFile> getEntityFiles(AbstractEntity entity, EntityFile parentDirectory) {
		return getEntityFiles(entity.getClass(), entity.getId(), parentDirectory);
	}

	@Override
	public List<EntityFile> getEntityFiles(Class clazz, Serializable id, EntityFile parentDirectory) {
		QueryParameters params = new QueryParameters();
		params.setAutocreateSearcheableStrings(false);
		params.add("targetEntity", QueryConditions.eq(clazz.getName()));
		if (id instanceof Long) {
			params.add("targetEntityId", QueryConditions.eq(id));
		} else {
			params.add("targetEntitySId", QueryConditions.eq(id.toString()));
		}
		params.add("state", QueryConditions.notEq(EntityFileState.DELETED));
		if (parentDirectory == null) {
			params.add("parent", QueryConditions.isNull());
		} else {
			params.add("parent", parentDirectory);
		}
		params.orderBy("type", true);
		return crudService.find(EntityFile.class, params);
	}

	private long counttEntityFiles(Class clazz, Serializable id) {
		QueryParameters params = new QueryParameters();
		params.setAutocreateSearcheableStrings(false);
		params.add("targetEntity", QueryConditions.eq(clazz.getName()));
		if (id instanceof Long) {
			params.add("targetEntityId", QueryConditions.eq(id));
		} else {
			params.add("targetEntitySId", QueryConditions.eq(id.toString()));
		}
		params.add("state", EntityFileState.VALID);
		params.add("type", EntityFileType.FILE);

		return crudService.count(EntityFile.class, params);
	}

	@Override
	public FilesConfig getConfiguration() {
		FilesConfig fileConfig = Containers.get().findObject(FilesConfig.class);
		if (fileConfig == null) {
			String loc = appParams.getValue(FilesConfig.FILES_LOCATION);
			if (loc != null && !loc.isEmpty()) {
				fileConfig = new FilesConfig(loc);
			}
		}

		if (fileConfig == null) {
			fileConfig = new FilesConfig();
		}
		return fileConfig;

	}

	@Override
	public InputStream download(EntityFile file) throws FileNotFoundException {
		if (file == null || file.getId() == null) {
			throw new FileNotFoundException("EntityFile is null");
		}

		File realFile = getRealFile(file);
		return new FileInputStream(realFile);
	}

	@Override
	@PostConstruct
	public void syncEntityFileAware() {
		List<String> targetEntities = crudService.getPropertyValues(EntityFile.class, "targetEntity");
		if (targetEntities != null) {
			logger.info("Syncing EntityFileAware entities");
			for (final String entityClassName : targetEntities) {
				Object object = BeanUtils.newInstance(entityClassName);
				if (object instanceof EntityFileAware) {
					crudService.executeWithinTransaction(new Task() {
						@Override
						public void doWork() {
							logger.info("Processing batch EntityFileAware for " + entityClassName);
							String updateQuery = "update "
									+ entityClassName
									+ " e set e.filesCount = (select count(ef.id) from EntityFile ef where ef.targetEntityId = e.id and ef.state = :state and ef.type = :type and ef.targetEntity='"
									+ entityClassName + "')";
							QueryParameters parameters = QueryParameters.with("state", EntityFileState.VALID)
									.add("type", EntityFileType.FILE);
							crudService.execute(updateQuery, parameters);
						}
					});

				}
			}
		}
	}

	@Override
	public File getRealFile(EntityFile file) {
		String filePath = getConfiguration().getRepository() + "/Account" + file.getAccount().getId() + "/" + file.getId();
		return new File(filePath);
	}

	private void processEntityFileAware(AbstractEntity target) {
		if (target instanceof EntityFileAware) {
			logger.info("Processing EntityFileAware for " + target.getClass() + " - " + target);
			EntityFileAware efa = (EntityFileAware) target;
			efa.setFilesCount(counttEntityFiles(target.getClass(), target.getId()));
			crudService.update(target);
		}
	}

}
