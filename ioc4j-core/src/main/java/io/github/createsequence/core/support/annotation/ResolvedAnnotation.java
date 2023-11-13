/*
 * Copyright (c) 2023 looly(loolly@aliyun.com)
 * Hutool is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          https://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package io.github.createsequence.core.support.annotation;

import io.github.createsequence.core.util.AnnotationUtils;
import io.github.createsequence.core.util.ArrayUtils;
import io.github.createsequence.core.util.Asserts;
import io.github.createsequence.core.util.ClassUtils;
import io.github.createsequence.core.util.CollectionUtils;
import io.github.createsequence.core.util.Graph;
import io.github.createsequence.core.util.ReflectUtils;
import io.github.createsequence.core.util.StringUtils;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * <p>属性解析注解，类似Spring的{@code MergedAnnotation}，用于包装并增强一个普通注解对象,
 * 包装后的可以通过{@code getResolvedXXX}获得注解对象或属性值，
 * 可以支持属性别名与属性覆写的属性解析机制。
 *
 * <p><strong>属性别名</strong>
 * <p>注解内的属性可以通过{@link AliasFor}互相关联，当解析时，
 * 对绑定中的任意一个属性的赋值，会被同步给其他直接或者间接关联的属性。<br>
 * eg: 若注解存在{@code a <=> b <=> c}的属性别名关系，则对<em>a</em>赋值，此时<em>b</em>、<em>c</em>也会被一并赋值。
 *
 * <p><strong>属性覆写</strong>
 * <p>当实例中{@link #source}不为{@code null}，即当前注解存在至少一个或者多个子注解时，
 * 若在子注解中的同名、同类型的属性，则获取值时将优先获取子注解的值，若该属性存在别名，则别名属性也如此。<br>
 * 属性覆写遵循如下机制：
 * <ul>
 *     <li>
 *         当覆写的属性存在别名属性时，别名属性也会一并被覆写；<br>
 *         eg: 若注解存在{@code a <=> b <=> c}的属性别名关系，则覆写<em>a</em>,，属性<em>b</em>、<em>c</em>也会被覆写；
 *     </li>
 *     <li>
 *         当属性可被多个子注解覆写时，总是优先选择离根注解最近的子注解覆写该属性；<br>
 *         eg：若从根注解<em>a</em>到元注解<em>b</em>有依赖关系{@code a => b => c}，
 *         此时若<em>c</em>中存在属性可同时被<em>a</em>、<em>b</em>覆写，则优先选择<em>a</em>；
 *     </li>
 *     <li>
 *         当覆写属性的子注解属性也被其子注解覆写时，等同于该子注解的子注解直接覆写的当前注解的属性；<br>
 *         eg：若从根注解<em>a</em>到元注解<em>b</em>有依赖关系{@code a => b => c}，
 *         此时若<em>b</em>中存在属性被<em>a</em>覆写，而<em>b</em>中被<em>a</em>覆写的属性又覆写<em>c</em>中属性，
 *         则等同于<em>c</em>中被覆写的属性直接被<em>a</em>覆写。
 *     </li>
 * </ul>
 *
 * @author huangchengxing
 * @see AliasFor
 */
@ToString(onlyExplicitlyIncluded = true)
public class ResolvedAnnotation implements Annotation {

	/**
	 * 不存在的属性对应的默认下标
	 */
	protected static final int NOT_FOUND_INDEX = -1;

	/**
	 * 注解属性，属性在该数组中的下标等同于属性本身
	 */
	@Getter
	private final Method[] attributes;

	/**
	 * 别名属性设置
	 */
	private final AliasSet[] aliasSets;

	/**
	 * 解析后的属性，下标都与{@link #attributes}相同下标的属性一一对应。
	 * 当下标对应属性下标不为{@link #NOT_FOUND_INDEX}时，说明该属性存在解析：
	 * <ul>
	 *     <li>若在{@link #resolvedAttributeSources}找不到对应实例，则说明该属性是别名属性；</li>
	 *     <li>若在{@link #resolvedAttributeSources}找的到对应实例，则说明该属性是覆盖属性；</li>
	 * </ul>
	 */
	private final int[] resolvedAttributes;

	/**
	 * 解析后的属性对应的数据源 <br>
	 * 当属性被覆写时，该属性对应下标位置会指向覆写该属性的注解对象
	 */
	private final ResolvedAnnotation[] resolvedAttributeSources;

	/**
	 * 当前注解的上一级注解对象，当该项为{@code null}时，则认为当前注解为根注解
	 */
	@Nullable
	private final ResolvedAnnotation source;

	/**
	 * 注解属性
	 */
	@ToString.Include
	@Getter
	@Delegate(types = Annotation.class)
	private final Annotation annotation;

	/**
	 * 代理对象缓存
	 */
	private Annotation proxied;

	/**
	 * 当前注解是否存在被解析的属性，当该值为{@code false}时，
	 * 通过{@code getResolvedAttributeValue}获得的值皆为注解的原始属性值，
	 * 通过{@link #synthesis()}获得注解对象为原始的注解对象。
	 */
	@ToString.Include
	@Getter
	private final boolean resolved;

	/**
	 * 当前注解是否由当前代理类生成
	 *
	 * @param annotation 注解对象
	 * @return 是否
	 */
	public static boolean isResolvedAnnotation(Annotation annotation) {
		return annotation instanceof ResolvedAnnotationInvocationHandler.Proxied;
	}

	/**
	 * 构建一个注解映射对象
	 *
	 * @param annotation                 注解对象
	 * @param resolveAnnotationAttribute 是否解析注解属性，为{@code true}时获得的注解皆支持属性覆盖与属性别名机制
	 * @return 注解映射对象
	 */
	public static ResolvedAnnotation create(Annotation annotation, boolean resolveAnnotationAttribute) {
		return create(null, annotation, resolveAnnotationAttribute);
	}

	/**
	 * 构建一个注解映射对象，子注解及子注解的子注解们的属性会覆写注解对象的中的同名同名同类型属性，
	 * 当一个属性被多个子注解覆写时，优先选择离根注解最接近的注解中的属性用于覆写，
	 *
	 * @param source 子注解
	 * @param annotation 注解对象
	 * @param resolveAnnotationAttribute 是否解析注解属性，为{@code true}时获得的注解皆支持属性覆盖与属性别名机制
	 * @return 注解映射对象
	 */
	public static ResolvedAnnotation create(
		@Nullable ResolvedAnnotation source, Annotation annotation, boolean resolveAnnotationAttribute) {
		return new ResolvedAnnotation(source, annotation, resolveAnnotationAttribute);
	}

	/**
	 * 构建一个注解映射对象
	 *
	 * @param source           当前注解的子注解
	 * @param annotation       注解对象
	 * @param resolveAttribute 是否需要解析属性
	 * @throws NullPointerException {@code source}为{@code null}时抛出
	 * @throws IllegalArgumentException
	 * <ul>
	 *     <li>当{@code annotation}已经被代理过时抛出；</li>
	 *     <li>当{@code source}包装的注解对象与{@code annotation}相同时抛出；</li>
	 *     <li>当{@code annotation}包装的注解对象类型为{@code ResolvedAnnotation}时抛出；</li>
	 * </ul>
	 */
	ResolvedAnnotation(
		@Nullable ResolvedAnnotation source, Annotation annotation, boolean resolveAttribute) {
		Objects.requireNonNull(annotation);
		Asserts.isFalse(isResolvedAnnotation(annotation), "annotation has been proxied");
		Asserts.isFalse(annotation instanceof ResolvedAnnotation, "annotation has been wrapped");
		Asserts.isFalse(
			Objects.nonNull(source) && Objects.equals(source.annotation, annotation),
			"The source annotation can not same with target [{}]", annotation
		);
		this.annotation = annotation;
		this.attributes = AnnotationUtils.getAnnotationAttributes(annotation.annotationType());
		this.source = source;

		// 别名属性
		this.aliasSets = new AliasSet[this.attributes.length];

		// 解析后的属性与数据源
		this.resolvedAttributeSources = new ResolvedAnnotation[this.attributes.length];
		this.resolvedAttributes = new int[this.attributes.length];
		Arrays.fill(this.resolvedAttributes, NOT_FOUND_INDEX);

		// 若有必要，解析属性
		this.resolved = resolveAttribute && resolveAttributes();
	}

	/**
	 * 解析属性
	 */
	protected boolean resolveAttributes() {
		// 解析同一注解中的别名
		resolveAliasAttributes();
		// 使用子注解覆写当前注解中的属性
		resolveOverwriteAttributes();
		// 注解的属性是否发生过解析
		return IntStream.of(resolvedAttributes)
			.anyMatch(idx -> NOT_FOUND_INDEX != idx);
	}

	// ================== 通用 ==================

	/**
	 * 当前注解是否为根注解
	 *
	 * @return 是否
	 */
	public boolean isRoot() {
		return Objects.isNull(source);
	}

	/**
	 * 获取根注解
	 *
	 * @return 根注解的映射对象
	 */
	public ResolvedAnnotation getRoot() {
		ResolvedAnnotation ra = this;
		while (Objects.nonNull(ra.source)) {
			ra = ra.source;
		}
		return ra;
	}

	/**
	 * 根据当前映射对象，通过动态代理生成一个合成注解，该注解相对原生注解：
	 * <ul>
	 *     <li>支持同注解内通过{@link AliasFor}构建的别名机制；</li>
	 *     <li>支持子注解对元注解的同名同类型属性覆盖机制；</li>
	 * </ul>
	 * 当{@link #isResolved()}为{@code false}时，则该方法返回被包装的原始注解对象。
	 *
	 * @return 所需的注解，若{@link ResolvedAnnotation#isResolved()}为{@code false}则返回的是原始的注解对象
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A synthesis() {
		if (!isResolved()) {
			return (A) annotation;
		}
		// 双重检查保证线程安全的创建代理缓存
		if (Objects.isNull(proxied)) {
			synchronized (this) {
				if (Objects.isNull(proxied)) {
					proxied = ResolvedAnnotationInvocationHandler.create(annotationType(), this);
				}
			}
		}
		return (A) proxied;
	}

	// ================== 属性搜索 ==================

	/**
	 * 注解是否存在指定属性
	 *
	 * @param attributeName 属性名称
	 * @param attributeType 属性类型
	 * @return 是否
	 */
	public boolean hasAttribute(String attributeName, Class<?> attributeType) {
		return getAttributeIndex(attributeName, attributeType) != NOT_FOUND_INDEX;
	}

	/**
	 * 该属性下标是否在注解中存在对应属性
	 *
	 * @param index 属性下标
	 * @return 是否
	 */
	public boolean hasAttribute(int index) {
		return index != NOT_FOUND_INDEX
			&& Objects.nonNull(ArrayUtils.get(attributes, index));
	}

	/**
	 * 获取注解属性的下标
	 *
	 * @param attributeName 属性名称
	 * @param attributeType 属性类型
	 * @return 属性下标
	 */
	public int getAttributeIndex(String attributeName, Class<?> attributeType) {
		for (int i = 0; i < attributes.length; i++) {
			Method attribute = attributes[i];
			if (Objects.equals(attribute.getName(), attributeName)
				&& ClassUtils.isAssignable(attributeType, attribute.getReturnType())) {
				return i;
			}
		}
		return NOT_FOUND_INDEX;
	}

	/**
	 * 根据下标获取注解属性
	 *
	 * @param index 属性下标
	 * @return 属性对象
	 */
	public Method getAttribute(int index) {
		return ArrayUtils.get(attributes, index);
	}

	// ================== 属性取值 ==================

	/**
	 * 获取属性值
	 *
	 * @param attributeName 属性名称
	 * @param attributeType 属性类型
	 * @param <R>           返回值类型
	 * @return 属性值
	 */
	public <R> R getAttributeValue(String attributeName, Class<R> attributeType) {
		return getAttributeValue(getAttributeIndex(attributeName, attributeType));
	}

	/**
	 * 获取属性值
	 *
	 * @param index 属性下标
	 * @param <R>   返回值类型
	 * @return 属性值
	 */
	public <R> R getAttributeValue(int index) {
		return hasAttribute(index) ? ReflectUtils.invokeRaw(annotation, attributes[index]) : null;
	}

	/**
	 * 获取解析后的属性值
	 *
	 * @param attributeName 属性名称
	 * @param attributeType 属性类型
	 * @param <R>           返回值类型
	 * @return 属性值
	 */
	public <R> R getResolvedAttributeValue(String attributeName, Class<R> attributeType) {
		return getResolvedAttributeValue(getAttributeIndex(attributeName, attributeType));
	}

	/**
	 * 获取解析后的属性值
	 *
	 * @param index 属性下标
	 * @param <R>   返回值类型
	 * @return 属性值
	 */
	public <R> R getResolvedAttributeValue(int index) {
		if (!hasAttribute(index)) {
			return null;
		}
		// 如果该属性没有经过解析，则直接获得原始值
		int resolvedIndex = resolvedAttributes[index];
		if (resolvedIndex == NOT_FOUND_INDEX) {
			return getAttributeValue(index);
		}
		// 若该属性被解析过，但是仍然还在当前实例中，则从实际属性获得值
		ResolvedAnnotation attributeSource = resolvedAttributeSources[index];
		if (Objects.isNull(attributeSource)) {
			return getAttributeValue(resolvedIndex);
		}
		// 若该属性被解析过，且不在本注解中，则从覆写它的注解中获得对应的值
		return attributeSource.getResolvedAttributeValue(resolvedIndex);
	}

	// ================== 解析覆写属性 ==================

	/**
	 * 令{@code annotationAttributes}中属性覆写当前注解中同名同类型的属性，
	 * 该步骤必须在{@link #resolveAliasAttributes()}后进行
	 */
	private void resolveOverwriteAttributes() {
		if (Objects.isNull(source)) {
			return;
		}
		// 获取除自己外的全部子注解
		Deque<ResolvedAnnotation> sources = new LinkedList<>();
		Set<Class<? extends Annotation>> accessed = new HashSet<>();
		accessed.add(this.annotationType());
		ResolvedAnnotation curr = this.source;
		while (Objects.nonNull(curr)) {
			// 检查循环依赖
			Asserts.isFalse(
				accessed.contains(curr.annotationType()),
				"Circular dependency between [{}] and [{}]",
				annotationType(), curr.annotationType()
			);
			// 尾插法，因此循环结束后，sources中头结点为根注解，而尾节点为当前注解
			sources.addFirst(curr);
			accessed.add(this.source.annotationType());
			curr = curr.source;
		}
		// 从根注解开始，依次覆写当前注解中的同名属性
		for (ResolvedAnnotation ra : sources) {
			updateResolvedAttributesByOverwrite(ra);
		}
	}

	/**
	 * 令{@code annotationAttributes}中属性覆写当前注解中同名、同类型且未被覆写的属性
	 *
	 *  @param overwriteAnnotation 当前注解的上级注解，即用于覆写当前注解属性的注解
	 */
	private void updateResolvedAttributesByOverwrite(ResolvedAnnotation overwriteAnnotation) {
		// 遍历覆写注解中的全部属性，然后依次与当前注解中的每一个属性进行匹配
		for (int overwriteIndex = 0; overwriteIndex < overwriteAnnotation.getAttributes().length; overwriteIndex++) {
			Method overwrite = overwriteAnnotation.getAttribute(overwriteIndex);
			for (int targetIndex = 0; targetIndex < attributes.length; targetIndex++) {
				Method attribute =  attributes[targetIndex];
				// 若有属性与当前属性名称与类型都一致，且未被覆写，则覆写该属性
				if (!Objects.equals(attribute.getName(), overwrite.getName())
					|| ClassUtils.isNotAssignable(attribute.getReturnType(), overwrite.getReturnType())) {
					continue;
				}
				overwriteAttribute(overwriteAnnotation, overwriteIndex, targetIndex, true);
			}
		}
	}

	/**
	 * 更新需要覆写的属性的相关映射关系，若该属性存在别名，则将别名的映射关系一并覆写
	 */
	private void overwriteAttribute(
		ResolvedAnnotation overwriteAnnotation, int overwriteIndex, int targetIndex, boolean overwriteAliases) {
		// 若目标属性已被覆写，则不允许再次覆写
		if (isOverwrittenAttribute(targetIndex)) {
			return;
		}
		// 覆写属性
		resolvedAttributes[targetIndex] = overwriteIndex;
		resolvedAttributeSources[targetIndex] = overwriteAnnotation;
		// 若覆写的属性本身还存在别名，则将别名属性一并覆写
		if (overwriteAliases && Objects.nonNull(aliasSets[targetIndex])) {
			aliasSets[targetIndex].forEach(aliasIndex -> overwriteAttribute(
				overwriteAnnotation, overwriteIndex, aliasIndex, false
			));
		}
	}

	/**
	 * 判断该属性是否已被覆写
	 */
	private boolean isOverwrittenAttribute(int index) {
		// 若属性未发生过解析，则必然未被覆写
		return NOT_FOUND_INDEX != resolvedAttributes[index]
			// 若属性发生过解析，且指向其他实例，则说明已被覆写
			&& Objects.nonNull(resolvedAttributeSources[index]);
	}

	// ================== 解析别名属性 ==================

	/**
	 * 解析当前注解属性中通过{@link AliasFor}构成别名的属性
	 */
	private void resolveAliasAttributes() {
		Map<Method, Integer> attributeIndexes = new HashMap<>(attributes.length);

		Graph<Method> methodGraph = new Graph<>();
		// 解析被作为别名的关联属性，根据节点关系构建邻接表
		for (int i = 0; i < attributes.length; i++) {
			// 获取属性上的@Alias注解
			Method attribute = attributes[i];
			attributeIndexes.put(attribute, i);
			AliasFor attributeAnnotation = attribute.getAnnotation(AliasFor.class);
			if (Objects.isNull(attributeAnnotation)) {
				continue;
			}
			// 获取别名属性
			Method aliasAttribute = getAliasAttribute(attribute, attributeAnnotation);
			Objects.requireNonNull(aliasAttribute);
			methodGraph.putEdge(aliasAttribute, attribute);
		}

		// 按广度优先遍历邻接表，将属于同一张图上的节点分为一组，并为其建立AliasSet
		Set<Method> accessed = new HashSet<>(attributes.length);
		Set<Method> group = new LinkedHashSet<>();
		Deque<Method> deque = new LinkedList<>();
		for (Method target : methodGraph.keySet()) {
			group.clear();
			deque.addLast(target);
			while (!deque.isEmpty()) {
				Method curr = deque.removeFirst();
				if (accessed.contains(curr)) {
					continue;
				}
				accessed.add(curr);
				// 将其添加到关系组
				group.add(curr);
				Collection<Method> aliases = methodGraph.getAdjacentPoints(curr);
				if (CollectionUtils.isNotEmpty(aliases)) {
					deque.addAll(aliases);
				}
			}
			// 为同一关系组的节点构建关联关系
			int[] groupIndexes = group.stream()
				.mapToInt(attributeIndexes::get)
				.toArray();
			updateAliasSetsForAliasGroup(groupIndexes);
		}

		// 根据AliasSet更新关联的属性
		Stream.of(aliasSets).filter(Objects::nonNull).forEach(set -> {
			int effectiveAttributeIndex = set.determineEffectiveAttribute();
			set.forEach(index -> resolvedAttributes[index] = effectiveAttributeIndex);
		});
	}

	/**
	 * 获取属性别名，并对其进行基本校验
	 */
	private Method getAliasAttribute(Method attribute, AliasFor aliasFor) {
		// 获取别名属性下标，该属性必须在当前注解中存在
		int aliasAttributeIndex = getAttributeIndex(aliasFor.value(), attribute.getReturnType());
		Asserts.isTrue(hasAttribute(aliasAttributeIndex), "Can not find alias attribute [{}] in [{}]", aliasFor.value(), this.annotation.annotationType());

		// 获取具体的别名属性，该属性不能是其本身
		Method aliasAttribute = getAttribute(aliasAttributeIndex);
		Asserts.isFalse(
			Objects.equals(aliasAttribute, attribute),
			"Attribute [{}] can not alias for itself", attribute
		);

		// 互为别名的属性类型必须一致
		Asserts.isTrue(
			ClassUtils.isAssignable(attribute.getReturnType(), aliasAttribute.getReturnType()),
			"Aliased attributes [{}] and [{}] must have same return type",
			attribute, aliasAttribute
		);
		return aliasAttribute;
	}

	/**
	 * 为具有关联关系的别名属性构建{@link AliasSet}
	 */
	private void updateAliasSetsForAliasGroup(int[] groupIndexes) {
		AliasSet set = new AliasSet(groupIndexes);
		for (int index : groupIndexes) {
			aliasSets[index] = set;
		}
	}

	/**
	 * 比较两个实例是否相等
	 *
	 * @param o 对象
	 * @return 是否
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ResolvedAnnotation that = (ResolvedAnnotation)o;
		return resolved == that.resolved && annotation.equals(that.annotation);
	}

	/**
	 * 获取实例哈希值
	 *
	 * @return 哈希值
	 */
	@Override
	public int hashCode() {
		return Objects.hash(annotation, resolved);
	}

	/**
	 * 别名设置，一组具有别名关系的属性会共用同一实例
	 */
	class AliasSet {

		/**
		 * 关联的别名字段对应的属性在{@link #attributes}中的下标
		 */
		int[] indexes;

		/**
		 * 创建一个别名设置
		 *
		 * @param indexes 互相关联的别名属性的下标
		 */
		AliasSet(int[] indexes) {
			this.indexes = indexes;
		}

		/**
		 * 从所有关联的别名属性中，选择出唯一个最终有效的属性：
		 * <ul>
		 *     <li>若所有属性都只有默认值，则要求所有的默认值都必须相等，若符合则返回首个属性，否则报错；</li>
		 *     <li>若有且仅有一个属性具有非默认值，则返回该属性；</li>
		 *     <li>若有多个属性具有非默认值，则要求所有的非默认值都必须相等，若符合并返回该首个具有非默认值的属性，否则报错；</li>
		 * </ul>
		 */
		private int determineEffectiveAttribute() {
			int resolvedIndex = NOT_FOUND_INDEX;
			boolean hasNotDef = false;
			Object lastValue = null;
			for (int index : indexes) {
				Method attribute = attributes[index];

				// 获取属性的值，并确认是否为默认值
				Object def = attribute.getDefaultValue();
				Object undef = ReflectUtils.invokeRaw(annotation, attribute);
				boolean isDefault = Objects.equals(def, undef);

				// 若是首个属性
				if (resolvedIndex == NOT_FOUND_INDEX) {
					resolvedIndex = index;
					lastValue = isDefault ? def : undef;
					hasNotDef = !isDefault;
					continue;
				}

				// 不是首个属性，且已存在非默认值
				if (hasNotDef) {
					// 如果当前也是非默认值，则要求两值必须相等
					if (!isDefault) {
						Asserts.isTrue(
							Objects.equals(lastValue, undef),
							"Aliased attribute [{}] and [{}] must have same not default value, but is different: [{}] <==> [{}]",
							attributes[resolvedIndex], attribute, lastValue, undef
						);
					}
					// 否则直接跳过，依然以上一非默认值为准
					continue;
				}

				// 不是首个属性，但是还没有非默认值，而当前值恰好是非默认值，直接更新当前有效值与对应索引
				if (!isDefault) {
					hasNotDef = true;
					lastValue = undef;
					resolvedIndex = index;
					continue;
				}

				// 不是首个属性，还没有非默认值，如果当前也是默认值，则要求两值必须相等
				Asserts.isTrue(
					Objects.equals(lastValue, def),
					"Aliased attribute [{}] and [{}] must have same default value, but is different: [{}] <==> [{}]",
					attributes[resolvedIndex], attribute, lastValue, def
				);
			}
			Asserts.isFalse(resolvedIndex == NOT_FOUND_INDEX, "Can not resolve aliased attributes from [{}]", annotation);
			return resolvedIndex;
		}

		/**
		 * 遍历下标
		 */
		void forEach(IntConsumer consumer) {
			for (int index : indexes) {
				consumer.accept(index);
			}
		}

		@Override
		public String toString() {
			return IntStream.of(indexes)
				.mapToObj(i -> attributes[i].getName())
				.collect(Collectors.joining(" <-> ", "[", "]"));
		}
	}

	/**
	 * 代理注解处理器，用于为{@link ResolvedAnnotation}生成代理对象，当从该代理对象上获取属性值时，
	 * 总是通过{@link ResolvedAnnotation#getResolvedAttributeValue(String, Class)}获取。
	 *
	 * @author huangchengxing
	 * @see ResolvedAnnotation
	 */
	static class ResolvedAnnotationInvocationHandler implements InvocationHandler {

		/**
		 * 属性映射
		 */
		private final ResolvedAnnotation annotation;

		/**
		 * 代理方法
		 */
		private final Map<String, BiFunction<Method, Object[], Object>> methods;

		/**
		 * 属性值缓存
		 */
		private final Map<String, Object> valueCache;

		/**
		 * 创建一个代理对象
		 *
		 * @param annotationType 注解类型
		 * @param annotation 已解析的注解对象
		 * @param <A> 注解类型
		 * @return 代理对象
		 */
		@SuppressWarnings("unchecked")
		public static <A extends Annotation> A create(Class<? extends A> annotationType, ResolvedAnnotation annotation) {
			Objects.requireNonNull(annotationType);
			Objects.requireNonNull(annotation);
			ResolvedAnnotationInvocationHandler invocationHandler = new ResolvedAnnotationInvocationHandler(annotation);
			return (A) java.lang.reflect.Proxy.newProxyInstance(
				annotationType.getClassLoader(),
				new Class[]{ annotationType, Proxied.class },
				invocationHandler
			);
		}

		/**
		 * 创建一个代理方法处理器
		 *
		 * @param annotation 属性映射
		 */
		private ResolvedAnnotationInvocationHandler(ResolvedAnnotation annotation) {
			int methodCount = annotation.getAttributes().length;
			this.methods = new HashMap<>(methodCount + 5);
			this.valueCache = new ConcurrentHashMap<>(methodCount);
			this.annotation = annotation;
			loadMethods();
		}

		/**
		 * 调用被代理的方法
		 *
		 * @param proxy  代理对象
		 * @param method 方法
		 * @param args   参数
		 * @return 返回值
		 */
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			return Optional.ofNullable(methods.get(method.getName()))
				.map(m -> m.apply(method, args))
				.orElseGet(() -> ReflectUtils.invokeRaw(annotation.getAnnotation(), method, args));
		}

		// ============================== 代理方法 ==============================

		/**
		 * 预加载需要代理的方法
		 */
		private void loadMethods() {
			methods.put("equals", (method, args) -> proxyEquals(args[0]));
			methods.put("toString", (method, args) -> proxyToString());
			methods.put("hashCode", (method, args) -> proxyHashCode());
			methods.put("annotationType", (method, args) -> proxyAnnotationType());
			methods.put("getAnnotation", (method, args) -> proxyGetAnnotation());
			for (Method attribute : annotation.getAttributes()) {
				methods.put(attribute.getName(), (method, args) -> getAttributeValue(method.getName(), method.getReturnType()));
			}
		}

		/**
		 * 代理{@link Annotation#toString()}方法
		 */
		private String proxyToString() {
			String attributes = Stream.of(annotation.getAttributes())
				.map(attribute -> StringUtils.format("{}={}", attribute.getName(), getAttributeValue(attribute.getName(), attribute.getReturnType())))
				.collect(Collectors.joining(", "));
			return StringUtils.format("@{}({})", annotation.annotationType().getName(), attributes);
		}

		/**
		 * 代理{@link Annotation#hashCode()}方法
		 */
		private int proxyHashCode() {
			return this.hashCode();
		}

		/**
		 * 代理{@link Annotation#equals(Object)}方法
		 */
		private boolean proxyEquals(Object o) {
			return Objects.equals(annotation, o);
		}

		/**
		 * 代理{@link Annotation#annotationType()}方法
		 */
		private Class<? extends Annotation> proxyAnnotationType() {
			return annotation.annotationType();
		}

		/**
		 * 代理{@link Proxied#getAnnotation()}方法
		 */
		private ResolvedAnnotation proxyGetAnnotation() {
			return annotation;
		}

		/**
		 * 获取属性值
		 */
		private Object getAttributeValue(String attributeName, Class<?> attributeType) {
			return valueCache.computeIfAbsent(attributeName, name -> annotation.getResolvedAttributeValue(attributeName, attributeType));
		}

		/**
		 * 表明注解是一个合成的注解
		 */
		interface Proxied {

			/**
			 * 获取注解映射对象
			 *
			 * @return 注解映射对象
			 */
			ResolvedAnnotation getAnnotation();

		}
	}
}
