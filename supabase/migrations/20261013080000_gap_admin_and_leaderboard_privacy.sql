-- GAP admin role and database-enforced leaderboard privacy.
-- The Test account receives a trusted app_metadata claim; client-editable user_metadata is never used for authorization.
DO $$
DECLARE
  target_user_id uuid;
  match_count integer;
BEGIN
  SELECT count(*), min(user_id) INTO match_count, target_user_id
  FROM public.gap_profiles
  WHERE lower(username) = 'test';

  IF match_count <> 1 THEN
    RAISE EXCEPTION 'Expected exactly one GAP profile named Test; found %', match_count;
  END IF;

  UPDATE auth.users
  SET raw_app_meta_data = COALESCE(raw_app_meta_data, '{}'::jsonb)
      || jsonb_build_object('gap_role', 'admin'),
      updated_at = now()
  WHERE id = target_user_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Could not grant admin role to the Test account';
  END IF;

  INSERT INTO public.gap_public_profiles (user_id, username, is_public, updated_at)
  SELECT user_id, username, false, now()
  FROM public.gap_profiles
  WHERE user_id = target_user_id
  ON CONFLICT (user_id) DO UPDATE
  SET is_public = false, updated_at = now();

  DELETE FROM public.gap_leaderboard_public
  WHERE user_id = target_user_id;
END $$;

-- Admins may inspect all GAP profiles and saved task data, but this is read-only.
DROP POLICY IF EXISTS "Admins can read all GALAKTOGON data" ON public.galaktogon_data;
CREATE POLICY "Admins can read all GALAKTOGON data"
ON public.galaktogon_data
FOR SELECT TO authenticated
USING ((select auth.jwt())->'app_metadata'->>'gap_role' = 'admin');

DROP POLICY IF EXISTS "Admins can read all GAP profiles" ON public.gap_profiles;
CREATE POLICY "Admins can read all GAP profiles"
ON public.gap_profiles
FOR SELECT TO authenticated
USING ((select auth.jwt())->'app_metadata'->>'gap_role' = 'admin');

-- A hidden profile is not queryable by other users through the public-profile API.
DROP POLICY IF EXISTS "Public profiles visible to signed-in users" ON public.gap_public_profiles;
CREATE POLICY "Public profiles visible to signed-in users"
ON public.gap_public_profiles
FOR SELECT TO authenticated
USING (
  is_public IS TRUE
  OR user_id = (select auth.uid())
  OR (select auth.jwt())->'app_metadata'->>'gap_role' = 'admin'
);

-- The leaderboard only exposes users who explicitly opted in.
DROP POLICY IF EXISTS "Leaderboard readable by authenticated users" ON public.gap_leaderboard_public;
CREATE POLICY "Leaderboard readable by authenticated users"
ON public.gap_leaderboard_public
FOR SELECT TO authenticated
USING (
  EXISTS (
    SELECT 1
    FROM public.gap_public_profiles p
    WHERE p.user_id = gap_leaderboard_public.user_id
      AND p.is_public IS TRUE
  )
);

-- Prevent a user from re-adding themselves to the leaderboard while opted out.
DROP POLICY IF EXISTS "Users can insert own leaderboard row" ON public.gap_leaderboard_public;
CREATE POLICY "Users can insert own leaderboard row"
ON public.gap_leaderboard_public
FOR INSERT TO authenticated
WITH CHECK (
  user_id = (select auth.uid())
  AND EXISTS (
    SELECT 1 FROM public.gap_public_profiles p
    WHERE p.user_id = (select auth.uid()) AND p.is_public IS TRUE
  )
);

DROP POLICY IF EXISTS "Users can update own leaderboard row" ON public.gap_leaderboard_public;
CREATE POLICY "Users can update own leaderboard row"
ON public.gap_leaderboard_public
FOR UPDATE TO authenticated
USING (user_id = (select auth.uid()))
WITH CHECK (
  user_id = (select auth.uid())
  AND EXISTS (
    SELECT 1 FROM public.gap_public_profiles p
    WHERE p.user_id = (select auth.uid()) AND p.is_public IS TRUE
  )
);
