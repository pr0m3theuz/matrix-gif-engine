# The test failed because the `modifiedBitboard` AFTER `undoUsePiecePotential` was:
# whitePUNCTLayer=[128, 0, 128, 0...] (expected [128, 0, 0, 0...])
# blackPUNCTLayer=[256, 384, 0...] (expected [256, 0, 0...])
#
# Let's see what `modifiedBitboard` was BEFORE `undoUsePiecePotential`:
# It was `postUsePotentialBitboardState`:
# whitePUNCTLayer = [128, 0, 0, 0, 0, 0, 0, 0]
# blackPUNCTLayer = [256, 256, 0, 0, 0, 0, 0, 0]
# Wait, why was `whitePUNCTLayer[0] = 128`?
# In `usePiecePotential`, `whitePUNCTLayer[0]` was 128, which is the sourceBit.
# But `usePiecePotential` now REMOVES it from `whitePUNCTLayer[0]`!
# If the test manually creates `modifiedBitboard` and assumes `whitePUNCTLayer[0] = 128` remains, the test is wrong!
# In my fix, `usePiecePotential` removes the piece from the source layer! So `whitePUNCTLayer[0]` should have been 0.
# And when `undoUsePiecePotential` runs, it ADDS it back to `whitePUNCTLayer[0]`.
# But since the test started with `whitePUNCTLayer[0] = 128`, it added it again, which is why it might have ended up placing it elsewhere?
# Oh! Because `whitePUNCTLayer[0]` already had 128, `safeAddAndCheck(..., sourceBit)` failed if it expected it empty.
# Wait! In my undo function, I look for an empty spot in the layers to add the sourceBit!
