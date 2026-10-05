-- Set when a director resets a child's password: the temporary password works once,
-- then the app asks the child to choose their own.
ALTER TABLE `children`
  ADD COLUMN `password_change_required` bit(1) NOT NULL DEFAULT b'0';
