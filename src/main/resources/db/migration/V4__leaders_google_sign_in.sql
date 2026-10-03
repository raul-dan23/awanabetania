-- "Continue with Google" for leaders.
--   email:      the Google address the director invites; a leader without one cannot use Google
--   google_sub: Google's permanent id for that account, bound on the first Google sign-in, so a
--               later change of the address on Google's side does not lock the leader out
-- One statement, so MySQL applies all of it or nothing.
ALTER TABLE `leaders`
  ADD COLUMN `email` varchar(255) DEFAULT NULL,
  ADD COLUMN `google_sub` varchar(255) DEFAULT NULL,
  ADD UNIQUE KEY `uk_leaders_email` (`email`),
  ADD UNIQUE KEY `uk_leaders_google_sub` (`google_sub`);
