package io.github.createsequence.core.util;

import io.github.createsequence.core.exception.Ioc4jException;
import io.github.createsequence.core.support.ParameterNameFinder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * ReflectUtils
 *
 * @author huangchengxing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReflectUtils {

    private static final String SET_PREFIX = "set";
    private static final String IS_PREFIX = "is";
    private static final String GET_PREFIX = "get";
    private static final Object[] EMPTY_PARAMS = new Object[0];

    /**
     * declared field cache
     */
    // TODO replace with WeakHashMap
    private static final Map<Class<?>, Field[]> DECLARED_FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * field cache
     */
    private static final Map<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * method cache
     */
    private static final Map<Class<?>, Method[]> DECLARED_METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * declared method cache
     */
    private static final Map<Class<?>, Method[]> METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * declared super class with interface
     */
    private static final Map<Class<?>, Set<Class<?>>> DECLARED_SUPER_CLASS_WITH_INTERFACE = new ConcurrentHashMap<>();

    // ====================== method ======================

    /**
     * {@code source}是否可以重写{@code target}，需要满足以下条件：
     * <ul>
     *     <li>方法名相同</li>
     *     <li>返回值类型相同或是子类</li>
     *     <li>参数数量相同，且每个参数的类型相同或是子类</li>
     * <ul>
     * @param source 源方法
     * @param target 目标方法，即被重写的方法
     * @return 是否
     */
    public static boolean isOverrideableFrom(Method source, Method target) {
        if (!Objects.equals(source.getName(), target.getName())) {
            return false;
        }
        if (!target.getReturnType().isAssignableFrom(source.getReturnType())) {
            return false;
        }
        if (source.getParameterCount() != target.getParameterCount()) {
            return false;
        }
        Class<?>[] childParameterTypes = source.getParameterTypes();
        Class<?>[] parentParameterTypes = target.getParameterTypes();
        for (int i = 0; i < childParameterTypes.length; i++) {
            if (ClassUtils.isNotAssignable(parentParameterTypes[i], childParameterTypes[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Invoke method.
     *
     * @param object object
     * @param method method to invoke
     * @param args arguments
     * @param <T> return type
     * @return return value of method invocation
     */
    @SuppressWarnings("all")
    public static <T> T invokeRaw(Object object, Method method, Object... args) {
        setAccessible(method);
        try {
            return (T) method.invoke(object, args);
        } catch (Throwable e) {
            e = (e instanceof InvocationTargetException) ?
                ((InvocationTargetException)e).getTargetException() : e;
            throw new Ioc4jException(e);
        }
    }

    /**
     * Invoke method.
     *
     * @param object object
     * @param method method to invoke
     * @param args arguments
     * @param <T> return type
     * @return return value of method invocation
     */
    public static <T> T invoke(Object object, Method method, Object... args) {
        Object[] actualArguments = resolveMethodInvocationArguments(method, args);
        return invokeRaw(object, method, actualArguments);
    }

    /**
     * Resolve invocation arguments of method.
     *
     * @param method method to invoke
     * @param args arguments
     * @return invocation arguments
     */
    public static Object[] resolveMethodInvocationArguments(Method method, Object... args) {
        int parameterCount = method.getParameterCount();
        if (parameterCount == 0) {
            return EMPTY_PARAMS;
        }
        // if args is null, return empty array
        if (ArrayUtils.isEmpty(args)) {
            return new Object[parameterCount];
        }
        // if the number of parameters is equal, return directly
        if (parameterCount == args.length) {
            return args;
        }
        // if the number of parameters is not equal, getResolvedAnnotation actual arguments
        Parameter[] parameters = method.getParameters();
        Object[] actualArguments = new Object[parameterCount];
        int newLen = Math.min(parameters.length, args.length);
        System.arraycopy(args, 0, actualArguments, 0, newLen);
        return actualArguments;
    }

    /**
     * Resolve method parameter names.
     *
     * @param finder discoverer
     * @param method method
     * @return collection of parameter name and parameter
     */
    @SuppressWarnings("all")
    public static Map<String, Parameter> resolveParameterNames(ParameterNameFinder finder, Method method) {
        Parameter[] parameters = method.getParameters();
        String[] parameterNames = finder.getParameterNames(method);
        if (ArrayUtils.isEmpty(parameters)) {
            return Collections.emptyMap();
        }
        Map<String, Parameter> parameterMap = new LinkedHashMap<>(parameters.length);
        int nameLength = ArrayUtils.length(parameterNames);
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String parameterName = nameLength <= i ? parameter.getName() : parameterNames[i];
            parameterMap.put(parameterName, parameter);
        }
        return parameterMap;
    }

    /**
     * Get declared methods of type.
     *
     * @param type type
     * @return method list
     */
    public static Method[] getDeclaredMethods(Class<?> type) {
        return CollectionUtils.computeIfAbsent(DECLARED_METHOD_CACHE, type, k -> type.getDeclaredMethods());
    }

    /**
     * Get method by name and parameter types.
     *
     * @param type type
     * @param methodName method name
     * @param parameterTypes parameter types
     * @return method if found, otherwise null
     */
    @Nullable
    public static Method getDeclaredMethod(
        Class<?> type, String methodName, @Nullable Class<?>... parameterTypes) {
        return findSpecificMethod(getDeclaredMethods(type), methodName, parameterTypes);
    }

    /**
     * Get methods.
     *
     * @param type type
     * @return method list
     * @see Class#getMethods()
     */
    public static Method[] getMethods(Class<?> type) {
        return CollectionUtils.computeIfAbsent(METHOD_CACHE, type, curr -> {
            List<Method> methods = new ArrayList<>();
            traverseTypeHierarchy(curr, t -> methods.addAll(Arrays.asList(getDeclaredMethods(t))));
            return methods.toArray(new Method[0]);
        });
    }

    /**
     * Get method by name and parameter types.
     *
     * @param type type
     * @param methodName method name
     * @param parameterTypes parameter types
     * @return method if found, otherwise null
     */
    @Nullable
    public static Method getMethod(
        Class<?> type, String methodName, @Nullable Class<?>... parameterTypes) {
        return findSpecificMethod(getMethods(type), methodName, parameterTypes);
    }

    /**
     * Find specific method by name and parameter types in method list.<br />
     * If parameterTypes:
     * <ul>
     *     <li>is null, only find method by name;</li>
     *     <li>is empty, find without parameter method by name;</li>
     *     <li>not empty, find method by name and parameters;</li>
     * </ul>
     *
     * @param methods method list
     * @param methodName method name
     * @param parameterTypes parameter types
     * @return method list
     */
    private static Method findSpecificMethod(Method[] methods, String methodName, Class<?>[] parameterTypes) {
        Predicate<Method> predicate = method -> method.getName().equals(methodName);
        if (Objects.nonNull(parameterTypes)) {
            predicate = parameterTypes.length > 0 ?
                predicate.and(method -> Arrays.equals(method.getParameterTypes(), parameterTypes)) :
                predicate.and(method -> method.getParameterCount() == 0);
        }
        return Stream.of(methods)
            .filter(predicate)
            .findFirst()
            .orElse(null);
    }

    /**
     * find getter method
     *
     * @param beanType bean's type
     * @param field field
     * @return java.util.Optional<java.lang.reflect.Method>
     */
    public static Optional<Method> findGetterMethod(Class<?> beanType, Field field) {
        // find isXXX method
        Class<?> fieldType = field.getType();
        if (boolean.class.equals(fieldType) || Boolean.class.equals(fieldType)) {
            String booleanGetterName = StringUtils.upperFirstAndAddPrefix(field.getName(), IS_PREFIX);
            return Optional.ofNullable(getMethod(beanType, booleanGetterName));
        }

        // find getXXX method
        String getterName = StringUtils.upperFirstAndAddPrefix(field.getName(), GET_PREFIX);
        Method method = getMethod(beanType, getterName);
        if (Objects.nonNull(method)) {
            return Optional.of(method);
        }

        // find fluent method
        getterName = field.getName();
        return Optional.ofNullable(getMethod(beanType, getterName));
    }

    /**
     * find getter method
     *
     * @param beanType bean's type
     * @param fieldName field's name
     * @return java.util.Optional<java.lang.reflect.Method>
     */
    public static Optional<Method> findGetterMethod(Class<?> beanType, String fieldName) {
        // find getXXX method
        String getterName = StringUtils.upperFirstAndAddPrefix(fieldName, GET_PREFIX);
        Method method = getMethod(beanType, getterName);
        if (Objects.nonNull(method)) {
            return Optional.of(method);
        }

        // find fluent method
        Optional<Method> fluentGetter = Optional.ofNullable(getMethod(beanType, fieldName));
        if (fluentGetter.isPresent()) {
            return fluentGetter;
        }

        // find isXXX method
        String booleanGetterName = StringUtils.upperFirstAndAddPrefix(fieldName, IS_PREFIX);
        return Optional.ofNullable(getMethod(beanType, booleanGetterName));
    }

    /**
     * find setter method
     *
     * @param beanType bean's type
     * @param field field
     * @return java.util.Optional<java.lang.reflect.Method>
     */
    public static Optional<Method> findSetterMethod(Class<?> beanType, Field field) {
        // find setXXX method
        Class<?> fieldType = field.getType();
        String setterName = StringUtils.upperFirstAndAddPrefix(field.getName(), SET_PREFIX);
        Method method = getMethod(beanType, setterName, fieldType);
        if (Objects.nonNull(method)) {
            return Optional.of(method);
        }

        // find fluent method
        setterName = field.getName();
        method = getMethod(beanType, setterName, fieldType);
        if (Objects.nonNull(method)) {
            return Optional.of(method);
        }

        // find isXXX method
        String booleanSetterName = StringUtils.upperFirstAndAddPrefix(field.getName(), IS_PREFIX);
        return Optional.ofNullable(getMethod(beanType, booleanSetterName, fieldType));
    }

    /**
     * find setter method
     *
     * @param beanType  bean's type
     * @param fieldName field's name
     * @return java.util.Optional<java.lang.reflect.Method>
     */
    public static Optional<Method> findSetterMethod(Class<?> beanType, String fieldName) {
        // find setXXX method
        String setterName = StringUtils.upperFirstAndAddPrefix(fieldName, SET_PREFIX);
        Optional<Method> method = findMethod(beanType, setterName, 1);
        if (method.isPresent()) {
            return method;
        }

        // find fluent method
        Optional<Method> fluentSetter = findMethod(beanType, fieldName, 1);
        if (fluentSetter.isPresent()) {
            return fluentSetter;
        }

        // find isXXX method
        String booleanSetterName = StringUtils.upperFirstAndAddPrefix(fieldName, IS_PREFIX);
        return findMethod(beanType, booleanSetterName, 1);
    }

    public static Optional<Method> findMethod(Class<?> beanType, String methodName, int parameterCount) {
        Method[] methods = getMethods(beanType);
        return Stream.of(methods)
            .filter(m -> m.getName().equals(methodName))
            .filter(m -> m.getParameterCount() == parameterCount)
            .findFirst();
    }

    // ====================== annotation ======================

    /**
     * Whether the {@code element} is createElement jdk.
     *
     * @param element element
     * @return boolean
     */
    public static boolean isJdkElement(AnnotatedElement element) {
        Class<?> checkedClass = element.getClass();
        if (element instanceof Class) {
            checkedClass = (Class<?>)element;
            if (checkedClass.isPrimitive()) {
                return true;
            }
        } else if (element instanceof Member member) {
            checkedClass = member.getDeclaringClass();
        }
        // then check package name is start with "javax." or "java."
        return ClassUtils.isJdkClass(checkedClass);
    }

    // ====================== class ======================

    /**
     * Get declared super class with interface.
     *
     * @param type type
     * @return declared super class with interface
     */
    public static Set<Class<?>> getDeclaredSuperClassWithInterface(Class<?> type) {
        return CollectionUtils.computeIfAbsent(DECLARED_SUPER_CLASS_WITH_INTERFACE, type, k -> {
            Set<Class<?>> result = new LinkedHashSet<>();
            Class<?> superClass = type.getSuperclass();
            if (superClass != null) {
                result.add(superClass);
            }
            result.addAll(Arrays.asList(type.getInterfaces()));
            return result;
        });
    }

    /**
     * Traverse type hierarchy.
     *
     * @param beanType bean type
     * @param consumer operation for each type
     */
    public static void traverseTypeHierarchy(Class<?> beanType, Consumer<Class<?>> consumer) {
        traverseTypeHierarchy(beanType, true, consumer);
    }

    /**
     * Traverse type hierarchy.
     *
     * @param beanType bean type
     * @param includeRoot whether include root type
     * @param consumer operation for each type
     */
    public static void traverseTypeHierarchy(Class<?> beanType, boolean includeRoot, Consumer<Class<?>> consumer) {
        Set<Class<?>> accessed = new HashSet<>();
        Deque<Class<?>> typeQueue = new LinkedList<>();
        typeQueue.add(beanType);
        while (!typeQueue.isEmpty()) {
            Class<?> type = typeQueue.removeFirst();
            accessed.add(type);
            // do something for current type
            if (includeRoot || type != beanType) {
                consumer.accept(type);
            }
            // then find superclass and interfaces
            Set<Class<?>> declaredSuperClassWithInterface = getDeclaredSuperClassWithInterface(type);
            declaredSuperClassWithInterface.remove(Object.class);
            declaredSuperClassWithInterface.removeAll(accessed);
            CollectionUtils.addAll(typeQueue, declaredSuperClassWithInterface);
        }
    }

    // ====================== field ======================

    /**
     * Get declared field by name.
     *
     * @param type type
     * @param fieldName field name
     * @return specified field, or null if not found
     */
    @Nullable
    public static Field getDeclaredField(Class<?> type, String fieldName) {
        return Stream.of(getDeclaredFields(type))
            .filter(f -> Objects.equals(f.getName(), fieldName))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get declared fields.
     *
     * @param type type
     * @return field array
     */
    public static Field[] getDeclaredFields(Class<?> type) {
        return CollectionUtils.computeIfAbsent(DECLARED_FIELD_CACHE, type, Class::getDeclaredFields);
    }

    /**
     * Get field by name.
     *
     * @param type type
     * @param fieldName field name
     * @return specified field, or null if not found
     */
    @Nullable
    public static Field getField(Class<?> type, String fieldName) {
        return Stream.of(getFields(type))
            .filter(f -> Objects.equals(f.getName(), fieldName))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get fields.
     *
     * @param type type
     * @return field array
     */
    public static Field[] getFields(Class<?> type) {
        return CollectionUtils.computeIfAbsent(FIELD_CACHE, type, t -> {
            List<Field> fields = new ArrayList<>();
            traverseTypeHierarchy(t, curr -> fields.addAll(Arrays.asList(getDeclaredFields(curr))));
            return fields.toArray(new Field[0]);
        });
    }

    /**
     * Get field value.
     *
     * @param target target object
     * @param fieldName field name
     * @param <T> field type
     * @return field value
     */
    @Nullable
    public static <T> T getFieldValue(Object target, String fieldName) {
        Field field = getField(target.getClass(), fieldName);
        return Objects.isNull(field) ? null : getFieldValue(target, field);
    }

    /**
     * Get field value.
     *
     * @param target target object
     * @param field field
     * @param <T> field type
     * @throws NullPointerException thrown when field is null
     * @return field value
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object target, Field field) {
        Objects.requireNonNull(field, "field must not null");
        setAccessible(field);
        try {
            return (T) field.get(target);
        } catch (Exception e) {
            throw new Ioc4jException(e);
        }
    }

    @SuppressWarnings("all")
    public static <T extends AccessibleObject> void setAccessible(T accessibleObject) {
        if (!accessibleObject.isAccessible()) {
            accessibleObject.setAccessible(true);
        }
    }
}
