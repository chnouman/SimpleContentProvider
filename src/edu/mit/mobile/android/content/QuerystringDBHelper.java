package edu.mit.mobile.android.content;
/*
 * Copyright (C) 2011 MIT Mobile Experience Lab
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import edu.mit.mobile.android.utils.ListUtils;

/**
 * <p>
 * Overrides the queryDir method in order to provide handling of select
 * statement building using URI query strings. To use, first construct a content
 * uri for a content item's directory. For example
 * <kbd>content://org.example.test/message</kbd>. Then add query parameters to
 * limit the result set (ideally, using
 * {@link android.net.Uri.Builder#appendQueryParameter(String, String)}) so that
 * your uri looks more like:
 * <kbd>content://org.example.test/message?to=steve</kbd>.
 * </p>
 *
 * <p>
 * This will translate the queries to select statements using the key for column
 * name and the value for the value, so you can easily provide links to specific
 * lists of your content items.
 * </p>
 *
 * <p>Keys and values are automatically escaped to prevent any SQL injections.</p>
 *
 * <p>
 * By default, multiple items are joined with AND, but can be joined by OR by
 * prefixing the query name with {@value #QUERY_PREFIX_OR}. For example:
 * <kbd>content://org.example.test/message?to=bob&amp;|to=alice
 * </p>
 *
 * @author <a href="mailto:spomeroy@mit.edu">Steve Pomeroy</a>
 */
// TODO provide ability to limit columns that can be queried.
public class QuerystringDBHelper extends GenericDBHelper {
	public static final String TAG = QuerystringDBHelper.class.getSimpleName();

	public static final String QUERY_PREFIX_OR = "|";

	public QuerystringDBHelper(Class<? extends ContentItem> contentItem,
			Uri contentUri) {
		super(contentItem, contentUri);
	}

	@Override
	public Cursor queryDir(SQLiteDatabase db, Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		final String query = uri.getEncodedQuery();
		String newSelection = selection;
		String[] newSelectionArgs = selectionArgs;
		Log.d(TAG, "query uri " + uri);
		try {
			if (query != null) {

				final StringBuilder sb = new StringBuilder();
				final List<NameValuePair> qs = URLEncodedUtils.parse(new URI(
						uri.toString()), "utf-8");
				// reset the URI for querying down the road
				uri = uri.buildUpon().query(null).build();

				final int count = qs.size();
				newSelectionArgs = new String[count];
				int i = 0;
				String name;
				for (final NameValuePair nvp : qs) {
					name = nvp.getName();

					if (i > 0) {
						if (name.startsWith(QUERY_PREFIX_OR)) {
							sb.append("OR ");
							name = name.substring(1);
						} else {
							sb.append("AND ");
						}
					}
					if (! SQLGenUtils.isValidName(name)){
						throw new SQLGenerationException("illegal column name in query: '"+name+"'");
					}
					// this isn't escaped, as we check it for validity.
					sb.append(name);
					sb.append("=? ");
					newSelectionArgs[i] = nvp.getValue();
					i++;
				}

				newSelection = ProviderUtils.addExtraWhere(selection,
						sb.toString());
				newSelectionArgs = ProviderUtils.addExtraWhereArgs(
						selectionArgs, newSelectionArgs);
				Log.d(TAG,
						"query:" + newSelection + "; args: ["
								+ ListUtils.join(Arrays.asList(newSelectionArgs), ",")+"]");
			}
		} catch (final URISyntaxException e) {
			final SQLGenerationException se = new SQLGenerationException("could not construct URL");
			se.initCause(e);
			throw se;
		}


		return super.queryDir(db, uri, projection, newSelection,
				newSelectionArgs, sortOrder);
	}
}
