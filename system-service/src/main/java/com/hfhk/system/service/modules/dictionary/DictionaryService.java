package com.hfhk.system.service.modules.dictionary;

import com.hfhk.cairo.core.Constants;
import com.hfhk.cairo.core.exception.UnknownBusinessException;
import com.hfhk.cairo.core.page.Page;
import com.hfhk.cairo.mongo.data.Metadata;
import com.hfhk.system.dictionary.domain.Dictionary;
import com.hfhk.system.service.constants.HfhkMongoProperties;
import com.hfhk.system.service.domain.mongo.DictionaryMongo;
import com.hfhk.system.service.modules.dictionary.domain.request.*;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j(topic = "[dictionary]")
@Service
public class DictionaryService {
	private final HfhkMongoProperties mongoProperties;
	private final MongoTemplate mongoTemplate;

	public DictionaryService(HfhkMongoProperties mongoProperties, MongoTemplate mongoTemplate) {
		this.mongoProperties = mongoProperties;
		this.mongoTemplate = mongoTemplate;
	}

	/**
	 * dictionary save
	 *
	 * @param params params
	 * @return dictionary optional
	 */
	public Optional<Dictionary> save(@NotNull String client, @Validated DictionarySaveParam params) {
		DictionaryMongo dictionaryMongo = DictionaryMongo.builder()
			.client(client)
			.code(Optional.ofNullable(params.getId()).orElse(Constants.SNOWFLAKE.nextIdStr()))
			.name(params.getName())
			.items(
				Optional.ofNullable(params.getItems())
					.stream()
					.flatMap(Collection::stream)
					.map(x ->
						DictionaryMongo.Item.builder()
							.id(Constants.SNOWFLAKE.nextIdStr())
							.value(Optional.ofNullable(x.getValue()).orElse(Constants.SNOWFLAKE.nextIdStr()))
							.name(x.getName())
							.metadata(new Metadata().setSort(Constants.SNOWFLAKE.nextId()))
							.build()
					)
					.collect(Collectors.toList())
			)
			.build();
		dictionaryMongo = mongoTemplate.insert(dictionaryMongo, mongoProperties.Collection.Dictionary);
		log.debug("[insert] result-> {}", dictionaryMongo);

		return find(client, params.getId());
	}

	/**
	 * dictionary modify
	 *
	 * @param param param
	 * @return 1
	 */
	public Optional<Dictionary> modify(@NotNull String client, @Validated DictionarySaveParam param) {
		final List<DictionaryMongo.Item> items = Optional.ofNullable(param.getItems())
			.stream()
			.flatMap(Collection::stream)
			.map(x ->
				DictionaryMongo.Item.builder()
					.id(Optional.ofNullable(x.getId()).orElse(Constants.SNOWFLAKE.nextIdStr()))
					.value(Optional.ofNullable(x.getValue()).orElse(Constants.SNOWFLAKE.nextIdStr()))
					.name(x.getName())
					.metadata(new Metadata().setSort(Constants.SNOWFLAKE.nextId()))
					.build()
			)
			.collect(Collectors.toList());
		Query query = Query.query(
			Criteria.where(DictionaryMongo.FIELD.CLIENT).is(client)
				.and(DictionaryMongo.FIELD.CODE).is(param.getId())
		);

		Update update = Update.update(DictionaryMongo.FIELD.Name, param.getName())
			.set(DictionaryMongo.FIELD.ITEMS.SELF, items);

		final UpdateResult updateResult = mongoTemplate.updateFirst(query, update, DictionaryMongo.class, mongoProperties.Collection.Dictionary);

		log.debug("[modify] result-> {}", updateResult);
		return find(client, param.getId());

	}

	/**
	 * dictionary delete
	 *
	 * @param client client
	 * @param param  param
	 * @return remove dictionary list
	 */
	public List<Dictionary> delete(@NotNull String client, @Validated DictionaryDeleteParam param) {
		Criteria criteria = Criteria
			.where(DictionaryMongo.FIELD.CLIENT).is(client)
			.and(DictionaryMongo.FIELD.CODE).in(param.getIds());

		Query query = Query.query(criteria);
		return mongoTemplate.findAllAndRemove(query, DictionaryMongo.class, mongoProperties.Collection.Dictionary)
			.stream()
			.filter(Objects::nonNull)
			.map(DictionaryConverter::mapper)
			.collect(Collectors.toList());
	}

	/**
	 * find
	 *
	 * @param client client
	 * @param param  query param
	 * @return x
	 */
	public List<Dictionary> find(@NotNull String client, @Validated DictionaryFindParam param) {
		Criteria criteria = buildDictionaryFindParamCriteria(client, param);
		Query query = Query.query(criteria).with(defaultSort());

		return mongoTemplate.find(query, DictionaryMongo.class, mongoProperties.Collection.Dictionary)
			.stream()
			.filter(Objects::nonNull)
			.map(DictionaryConverter::mapper)
			.collect(Collectors.toList());
	}

	/**
	 * find page
	 *
	 * @param client client
	 * @param param  query param
	 * @return x
	 */
	public Page<Dictionary> findPage(@NotNull String client, @Validated DictionaryFindParam param) {
		Criteria criteria = buildDictionaryFindParamCriteria(client, param);

		Query query = Query.query(criteria);
		long total = mongoTemplate.count(query, DictionaryMongo.class, mongoProperties.Collection.Dictionary);

		query.with(param.pageable()).with(defaultSort());
		List<Dictionary> contents = mongoTemplate.find(query, DictionaryMongo.class, mongoProperties.Collection.Dictionary)
			.stream()
			.filter(Objects::nonNull)
			.map(DictionaryConverter::mapper)
			.collect(Collectors.toList());
		return new Page<>(param, contents, total);
	}

	/**
	 * put Item
	 *
	 * @param params params
	 * @return dictionary optional
	 */
	public Optional<Dictionary> putItems(@NotNull String client, @Validated DictionaryItemPutParam params) {
		final Criteria criteria = Criteria
			.where(DictionaryMongo.FIELD.CLIENT).is(client)
			.and(DictionaryMongo.FIELD.CODE).is(params.getId());

		final List<DictionaryMongo.Item> newItems = Optional
			.ofNullable(params.getItems())
			.stream()
			.flatMap(Collection::stream)
			.map(x -> DictionaryMongo.Item.builder()
				.id(Optional.ofNullable(x.getId()).orElse(Constants.SNOWFLAKE.nextIdStr()))
				.value(x.getValue())
				.name(x.getName())
				.metadata(new Metadata().setSort(Constants.SNOWFLAKE.nextId()))
				.build())
			.collect(Collectors.toList());

		Query query = Query.query(criteria);

		Update update = new Update().addToSet(DictionaryMongo.FIELD.ITEMS.SELF).each(newItems);

		final UpdateResult updateResult = mongoTemplate.updateFirst(query, update, DictionaryMongo.class, mongoProperties.Collection.Dictionary);
		log.debug("[dictionary][putItem] result-> : {}", updateResult);

		return find(client, params.getId());
	}

	/**
	 * dictionary item modify
	 *
	 * @param param param
	 * @return dictionary optional
	 */
	public Optional<Dictionary> modifyItem(@NotNull String client, @Validated DictionaryItemModifyParam param) {
		final Criteria criteria = Criteria
			.where(DictionaryMongo.FIELD.CLIENT).is(client)
			.and(DictionaryMongo.FIELD.CODE).is(param.getId())
			.and(DictionaryMongo.FIELD.ITEMS.ID).in(param.getItem().getId());
		final Query query = Query.query(criteria);

		final Update update = new Update()
			.set(DictionaryMongo.FIELD.ITEMS.$VALUE, param.getItem().getValue())
			.set(DictionaryMongo.FIELD.ITEMS.$NAME, param.getItem().getName());

		final UpdateResult updateResult = mongoTemplate.updateFirst(query, update, DictionaryMongo.class, mongoProperties.Collection.Dictionary);
		log.debug("[dictionary][putItem] result-> : {}", updateResult);

		return find(client, param.getId());
	}

	/**
	 * dictionary item delete
	 *
	 * @param param param
	 * @return dictionary optional
	 */
	public Optional<Dictionary> deleteItems(@NotNull String client, @Validated DictionaryItemDeleteParam param) {
		final Criteria criteria = Criteria
			.where(DictionaryMongo.FIELD.CLIENT).is(client);
		Optional.of(param)
			.ifPresent(x -> Optional
				.ofNullable(x.getId())
				.ifPresentOrElse(
					id -> criteria.and(DictionaryMongo.FIELD.CODE).is(id),
					() -> {
						throw new UnknownBusinessException("Id不能为空");
					}
				)
			);

		final Query query = Query.query(criteria);

		final Update update = new Update();
		Optional.ofNullable(param.getItemIds())
			.ifPresentOrElse(
				itemIds -> update.pullAll(DictionaryMongo.FIELD.ITEMS.ID, itemIds.toArray()),
				() -> {
					throw new UnknownBusinessException("ItemId为空");
				}
			);
		update.pullAll(DictionaryMongo.FIELD.ITEMS.ID, param.getItemIds().toArray(new String[0]));

		final UpdateResult updateResult = mongoTemplate.updateFirst(query, update, DictionaryMongo.class, mongoProperties.Collection.Dictionary);
		log.debug("[dictionary][deleteItem] result-> : {}", updateResult);

		return find(client, param.getId());
	}

	Optional<Dictionary> find(String client, String id) {
		Criteria criteria = Criteria.where(DictionaryMongo.FIELD.CLIENT).is(client).and(DictionaryMongo.FIELD.CODE).is(id);
		Query query = Query.query(criteria);
		return Optional.ofNullable(mongoTemplate.findOne(query, DictionaryMongo.class, mongoProperties.Collection.Dictionary))
			.map(DictionaryConverter::mapper);
	}

	Criteria buildDictionaryFindParamCriteria(@NotNull String client, @Validated DictionaryFindParam param) {
		Criteria criteria = Criteria.where(DictionaryMongo.FIELD.CLIENT).is(client);
		Optional.ofNullable(param.getCode()).ifPresent(y -> criteria.and(DictionaryMongo.FIELD.CODE).regex(y));
		Optional.ofNullable(param.getName()).ifPresent(y -> criteria.and(DictionaryMongo.FIELD.Name).regex(y));
		return criteria;
	}

	Sort defaultSort() {
		return Sort.by(
			Sort.Order.asc(DictionaryMongo.FIELD.METADATA.SORT),
			Sort.Order.asc(DictionaryMongo.FIELD.METADATA.CREATED.AT),
			Sort.Order.asc(DictionaryMongo.FIELD._ID),
			Sort.Order.asc(DictionaryMongo.FIELD.ITEMS.METADATA.SORT),
			Sort.Order.asc(DictionaryMongo.FIELD.ITEMS.VALUE),
			Sort.Order.asc(DictionaryMongo.FIELD.ITEMS.ID)
		);
	}


}
