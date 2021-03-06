/**
 * This file is part of PaxmlCore.
 *
 * PaxmlCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PaxmlCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PaxmlCore.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.paxml.security;

import org.paxml.util.CryptoUtils;

public class Secret {

	private final String encrypted;
	private final String name;

	public Secret(String name, String clearSecret) {
		encrypted = CryptoUtils.base64Encode(CryptoUtils.encrypt(clearSecret, SecretRepository.getCurrentUserMasterKey()));
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getEncrypted() {
		return encrypted;
	}

	public String getDecrypted() {
		return CryptoUtils.decrypt(CryptoUtils.base64Decode(encrypted), SecretRepository.getCurrentUserMasterKey());
	}

	@Override
	public String toString() {
		return "******";
	}
}
