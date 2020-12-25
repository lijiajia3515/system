package com.hfhk.system.service.modules.file.service;

import com.hfhk.cairo.core.page.Page;
import com.hfhk.system.file.domain.Folder;
import com.hfhk.system.file.domain.request.FolderPageFindRequest;

import java.util.List;

/**
 * 文件夹
 */
public interface FolderService {

	/**
	 * 创建
	 *
	 * @param client   client
	 * @param filepath filepath
	 */
	void create(String client, String filepath);


	/**
	 * 修改
	 *
	 * @param client      client
	 * @param filepath    filepath
	 * @param newFilepath new filepath
	 */
	void rename(String client, String filepath, String newFilepath);

	/**
	 * 文件夹删除
	 *
	 * @param client   client
	 * @param filepath filepath
	 */
	void delete(String client, String filepath);


	/**
	 * 查询
	 *
	 * @param client  client
	 * @param request request
	 * @return filepath
	 */
	Page<String> pageFind(String client, FolderPageFindRequest request);

	/**
	 * 查询
	 *
	 * @param client client
	 * @return x
	 */
	List<Folder> treeFind(String client, String filepath);

}