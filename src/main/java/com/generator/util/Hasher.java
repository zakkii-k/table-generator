package com.generator.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCryptアルゴリズムを使用してパスワードをハッシュ化するユーティリティクラス。
 */
public class Hasher {
    // BCryptのストレッチング因子 (強度の設定)
    private static final int LOG_ROUNDS = 10;

    /**
     * 指定されたプレーンテキストパスワードをBCryptでハッシュ化します。
     * @param password ハッシュ化するパスワード
     * @return ハッシュ化されたパスワード文字列
     */
    public static String hashPassword(String password) {
        // パスワードをソルト（ランダム値）と組み合わせてハッシュ化
        return BCrypt.hashpw(password, BCrypt.gensalt(LOG_ROUNDS));
    }
}
