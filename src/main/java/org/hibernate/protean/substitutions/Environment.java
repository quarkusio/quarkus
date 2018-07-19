package org.hibernate.protean.substitutions;

import java.util.Map;
import java.util.Properties;

import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.protean.entityenhancement.UpfrontDefinedBytecodeProvider;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.hibernate.cfg.Environment")
@Substitute
public final class Environment implements AvailableSettings {

	private static final BytecodeProvider BYTECODE_PROVIDER_INSTANCE = new UpfrontDefinedBytecodeProvider();

	@Substitute
	public static void verifyProperties(Map<?,?> configurationValues) {
	}

	@Substitute
	public static boolean useStreamsForBinary() {
		return false;
	}

	@Substitute
	public static Properties getProperties() {
		return new Properties();
	}

	//Required even if deprecated in the original class: Works around a bug in Substrate now complaining about the right method name.
	@Substitute
	public static BytecodeProvider getBytecodeProvider() {
		return BYTECODE_PROVIDER_INSTANCE;
	}

	@Substitute
	public static BytecodeProvider buildBytecodeProvider(Properties properties) {
		return BYTECODE_PROVIDER_INSTANCE;
	}

}
