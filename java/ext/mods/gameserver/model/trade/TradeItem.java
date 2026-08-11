/*
* Copyleft © 2024-2026 L2Lineternity
* * This file is part of L2Lineternity derived from aCis409/RusaCis3.8
* * L2Lineternity is free software: you can redistribute it and/or modify it
* under the terms of the GNU General Public License as published by the
* Free Software Foundation, either version 3 of the License.
* * L2Lineternity is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* General Public License for more details.
* * You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
* Our main Developers, Dhousefe-L2JBR, Agazes33, Ban-L2jDev, Warman, SrEli.
* Our special thanks, Nattan Felipe, Diego Fonseca, Junin, ColdPlay, Denky, MecBew, Localhost, MundvayneHELLBOY, 
* SonecaL2, Eduardo.SilvaL2J, biLL, xpower, xTech, kakuzo, Tiagorosendo, Schuster, LucasStark, damedd
* as a contribution for the forum L2JBrasil.com
 */
package ext.mods.gameserver.model.trade;

import ext.mods.gameserver.model.item.instance.ItemInstance;
import ext.mods.gameserver.model.item.kind.Item;

public class TradeItem extends ItemRequest
{
	private Item _item;
	private int _quantity;
	private int _customType1;
	private int _customType2;
	
	public TradeItem(ItemInstance item, int count, int price)
	{
		super(item.getObjectId(), item.getItem().getItemId(), count, price, item.getEnchantLevel());
		
		_item = item.getItem();
		_quantity = count;
		_customType1 = item.getCustomType1();
		_customType2 = item.getCustomType2();
	}
	
	public TradeItem(Item item, int count, int price, int enchant)
	{
		super(0, item.getItemId(), count, price, enchant);
		
		_item = item;
		_quantity = count;
		_customType1 = 0;
		_customType2 = 0;
	}
	
	public TradeItem(TradeItem item, int count, int price)
	{
		super(item.getObjectId(), item.getItemId(), count, price, item.getEnchant());
		
		_item = item.getItem();
		_quantity = count;
		_customType1 = item.getCustomType1();
		_customType2 = item.getCustomType2();
	}
	
	@Override
	public String toString()
	{
		return "TradeItem [item=" + _item + ", quantity=" + _quantity + ", objectId=" + _objectId + ", itemId=" + _itemId + ", count=" + _count + ", price=" + _price + ", enchant=" + _enchant + "]";
	}
	
	public Item getItem()
	{
		return _item;
	}
	
	public int getCustomType1()
	{
		return _customType1;
	}
	
	public int getCustomType2()
	{
		return _customType2;
	}
	
	public int getQuantity()
	{
		return _quantity;
	}
	
	public void setQuantity(int quantity)
	{
		_quantity = quantity;
	}
}