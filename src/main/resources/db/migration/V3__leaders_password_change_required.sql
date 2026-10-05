-- Same as V2, for leaders whose password the director resets.
ALTER TABLE `leaders`
  ADD COLUMN `password_change_required` bit(1) NOT NULL DEFAULT b'0';
