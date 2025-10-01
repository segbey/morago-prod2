CREATE INDEX idx_tp_online   ON translator_profiles(is_online);
CREATE INDEX idx_tp_verified ON translator_profiles(is_verified);

CREATE INDEX idx_tl_lang   ON translator_languages(language_id, translator_profile_id);

CREATE INDEX idx_tt_theme  ON translator_themes(theme_id, translator_profile_id);
