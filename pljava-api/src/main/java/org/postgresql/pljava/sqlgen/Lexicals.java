/*
 * Copyright (c) 2015-2016 Tada AB and other contributors, as listed below.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the The BSD 3-Clause License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Contributors:
 *   Chapman Flack
 */
package org.postgresql.pljava.sqlgen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A few useful SQL lexical definitions supplied as {@link Pattern} objects.
 *
 * The idea is not to go overboard and reimplement an SQL lexer, but to
 * capture in one place the rules for those bits of SQL snippets that are
 * likely to be human-supplied in annotations and need to be checked for
 * correctness when emitted into deployment descriptors. For starters, that
 * means regular (not quoted, not Unicode escaped) identifiers.
 *
 * Supplied in the API module so they are available to {@code javac} to
 * compile and generate DDR when the rest of PL/Java is not necessarily
 * present. Of course backend code such as {@code SQLDeploymentDescriptor}
 * can also refer to these.
 */
public abstract class Lexicals {

	/** Allowed as the first character of a regular identifier by ISO.
	 */
	public static final Pattern ISO_REGULAR_IDENTIFIER_START = Pattern.compile(
		"[\\p{Lu}\\p{Ll}\\p{Lt}\\p{Lm}\\p{Lo}\\p{Nl}]"
	);

	/** Allowed as any non-first character of a regular identifier by ISO.
	 */
	public static final Pattern ISO_REGULAR_IDENTIFIER_PART =
	Pattern.compile(String.format(
		"[\\xb7\\p{Mn}\\p{Mc}\\p{Nd}\\p{Pc}\\p{Cf}%1$s]",
		ISO_REGULAR_IDENTIFIER_START.pattern()
	));

	/** A complete regular identifier as allowed by ISO.
	 */
	public static final Pattern ISO_REGULAR_IDENTIFIER =
	Pattern.compile(String.format(
		"%1$s%2$s{0,127}+",
		ISO_REGULAR_IDENTIFIER_START.pattern(),
		ISO_REGULAR_IDENTIFIER_PART.pattern()
	));

	/** A complete ISO regular identifier in a single capturing group.
	 */
	public static final Pattern ISO_REGULAR_IDENTIFIER_CAPTURING =
	Pattern.compile(String.format(
		"(%1$s)", ISO_REGULAR_IDENTIFIER.pattern()
	));

	/** A complete delimited identifier as allowed by ISO. As it happens, this
	 * is also the form PostgreSQL uses for elements of a LIST_QUOTE-typed GUC.
	 */
	public static final Pattern ISO_DELIMITED_IDENTIFIER = Pattern.compile(
		"\"(?:[^\"]|\"\"){1,128}+\""
	);

	/** An ISO delimited identifier with a single capturing group that captures
	 * the content (which still needs to have "" replaced with " throughout).
	 * The capturing group is named {@code xd}.
	 */
	public static final Pattern ISO_DELIMITED_IDENTIFIER_CAPTURING =
	Pattern.compile(String.format(
		"\"(?<xd>(?:[^\"]|\"\"){1,128}+)\""
	));

	/** The escape-specifier part of a Unicode delimited identifier or string.
	 * The escape character itself is in the capturing group named {@code uec}.
	 * The group can be absent, in which case \ should be used as the uec.
	 */
	public static final Pattern ISO_UNICODE_ESCAPE_SPECIFIER =
	Pattern.compile(
		"(?:\\p{IsWhite_Space}*+[Uu][Ee][Ss][Cc][Aa][Pp][Ee]"+
		"\\p{IsWhite_Space}*+'(?<uec>[^0-9A-Fa-f+'\"\\p{IsWhite_Space}])')?+"
	);

	/** A Unicode delimited identifier. The body is in capturing group
	 * {@code xui} and the escape character in group {@code uec}. The body
	 * still needs to have "" replaced with ", and {@code Unicode escape value}s
	 * decoded and replaced, and then it has to be verified to be no longer
	 * than 128 codepoints.
	 */
	public static final Pattern ISO_UNICODE_IDENTIFIER =
	Pattern.compile(String.format(
		"[Uu]&\"(?<xui>(?:[^\"]|\"\")++)\"%1$s",
		ISO_UNICODE_ESCAPE_SPECIFIER.pattern()
	));

	/** A compilable pattern to match a {@code Unicode escape value}.
	 * A match should have one of three named capturing groups. If {@code cev},
	 * substitute the {@code uec} itself. If {@code u4d} or {@code u6d},
	 * substitute the codepoint represented by the hex digits. A match with none
	 * of those capturing groups indicates an ill-formed string.
	 *<p>
	 * Maka a Pattern from this by supplying the right {@code uec}, so:
	 * {@code Pattern.compile(String.format(ISO_UNICODE_REPLACER,
	 *   Pattern.quote(uec)));}
	 */
	public static final String ISO_UNICODE_REPLACER =
		"%1$s(?:(?<cev>%1$s)|(?<u4d>[0-9A-Fa-f]{4})|\\+(?<u6d>[0-9A-Fa-f]{6}))";

	/** Allowed as the first character of a regular identifier by PostgreSQL
	 * (PG 7.4 -).
	 */
	public static final Pattern PG_REGULAR_IDENTIFIER_START = Pattern.compile(
		"[A-Za-z\\P{ASCII}_]" // hasn't seen a change since PG 7.4
	);

	/** Allowed as any non-first character of a regular identifier by PostgreSQL
	 * (PG 7.4 -).
	 */
	public static final Pattern PG_REGULAR_IDENTIFIER_PART =
	Pattern.compile(String.format(
		"[0-9$%1$s]", PG_REGULAR_IDENTIFIER_START.pattern()
	));

	/** A complete regular identifier as allowed by PostgreSQL (PG 7.4 -).
	 */
	public static final Pattern PG_REGULAR_IDENTIFIER =
	Pattern.compile(String.format(
		"%1$s%2$s*+",
		PG_REGULAR_IDENTIFIER_START.pattern(),
		PG_REGULAR_IDENTIFIER_PART.pattern()
	));

	/** A complete PostgreSQL regular identifier in a single capturing group.
	 */
	public static final Pattern PG_REGULAR_IDENTIFIER_CAPTURING =
	Pattern.compile(String.format(
		"(%1$s)", PG_REGULAR_IDENTIFIER.pattern()
	));

	/** A regular identifier that satisfies both ISO and PostgreSQL rules.
	 */
	public static final Pattern ISO_AND_PG_REGULAR_IDENTIFIER =
	Pattern.compile(String.format(
		"(?:(?=%1$s)%2$s)(?:(?=%3$s)%4$s)*+",
		ISO_REGULAR_IDENTIFIER_START.pattern(),
		PG_REGULAR_IDENTIFIER_START.pattern(),
		ISO_REGULAR_IDENTIFIER_PART.pattern(),
		PG_REGULAR_IDENTIFIER_PART.pattern()
	));

	/** A regular identifier that satisfies both ISO and PostgreSQL rules,
	 * in a single capturing group named {@code i}.
	 */
	public static final Pattern ISO_AND_PG_REGULAR_IDENTIFIER_CAPTURING =
	Pattern.compile(
		String.format( "(?<i>%1$s)", ISO_AND_PG_REGULAR_IDENTIFIER.pattern())
	);

	/** Pattern that matches any identifier valid by both ISO and PG rules,
	 * with the presence of named capturing groups indicating which kind it is:
	 * {@code i} for a regular identifier, {@code xd} for a delimited identifier
	 * (still needing "" replaced with "), or {@code xui} (with or without an
	 * explicit {@code uec} for a Unicode identifier (still needing "" to " and
	 * decoding of {@code Unicode escape value}s).
	 */
	public static final Pattern ISO_AND_PG_IDENTIFIER_CAPTURING =
	Pattern.compile(String.format(
		"%1$s|(?:%2$s)|(?:%3$s)",
		ISO_AND_PG_REGULAR_IDENTIFIER_CAPTURING.pattern(),
		ISO_DELIMITED_IDENTIFIER_CAPTURING.pattern(),
		ISO_UNICODE_IDENTIFIER.pattern()
	));

	/** An identifier by ISO SQL, PostgreSQL, <em>and</em> Java (not SQL at all)
	 * rules. (Not called {@code REGULAR} because Java allows no other form of
	 * identifier.) This restrictive form is the safest for identifiers being
	 * generated into a deployment descriptor file that an old version of
	 * PL/Java might load, because through 1.4.3 PL/Java used the Java
	 * identifier rules to recognize identifiers in deployment descriptors.
	 */
	public static final Pattern ISO_PG_JAVA_IDENTIFIER =
	Pattern.compile(String.format(
		"(?:(?=%1$s)(?=\\p{%5$sStart})%2$s)(?:(?=%3$s)(?=\\p{%5$sPart})%4$s)*+",
		ISO_REGULAR_IDENTIFIER_START.pattern(),
		PG_REGULAR_IDENTIFIER_START.pattern(),
		ISO_REGULAR_IDENTIFIER_PART.pattern(),
		PG_REGULAR_IDENTIFIER_PART.pattern(),
		"javaJavaIdentifier"
	));

	/**
	 * Return an identifier, given a {@code Matcher} that has matched an
	 * ISO_AND_PG_IDENTIFIER_CAPTURING. Will determine from the matching named
	 * groups which type of identifier it was, process the matched sequence
	 * appropriately, and return it.
	 * @param m A {@code Matcher} known to have matched an identifier.
	 * @return the recovered identifier string.
	 */
	public static String identifierFrom(Matcher m)
	{
		String s = m.group("i");
		if ( null != s )
			return s;
		s = m.group("xd");
		if ( null != s )
			return s.replace("\"\"", "\"");
		s = m.group("xui");
		if ( null == s )
			return null; // XXX?
		s = s.replace("\"\"", "\"");
		String uec = m.group("uec");
		if ( null == uec )
			uec = "\\";
		int uecp = uec.codePointAt(0);
		Matcher replacer =
			Pattern.compile(
				String.format(ISO_UNICODE_REPLACER, Pattern.quote(uec)))
				.matcher(s);
		StringBuffer sb = new StringBuffer();
		while ( replacer.find() )
		{
			replacer.appendReplacement(sb, "");
			int cp;
			String uev = replacer.group("u4d");
			if ( null == uev )
				uev = replacer.group("u6d");
			if ( null != uev )
				cp = Integer.parseInt(uev, 16);
			else
				cp = uecp;
			// XXX check validity
			sb.appendCodePoint(cp);
		}
		return replacer.appendTail(sb).toString();
	}
}
